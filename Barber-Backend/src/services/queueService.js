const BarberQueue = require('../models/BarberQueue');
const { broadcastQueueEvent } = require('./socketService');

/**
 * Parses time string like "5:30 PM", "05:30 PM", "11:00 AM" into minutes of day (0..1439).
 */
const parseTimeToMinutesOfDay = (timeStr) => {
  if (!timeStr) return 0;
  const clean = timeStr.trim().toUpperCase();
  const isPm = clean.includes('PM');
  const timePart = clean.replace('AM', '').replace('PM', '').trim();
  const parts = timePart.split(':');
  if (parts.length === 0) return 0;

  let hours = parseInt(parts[0], 10) || 0;
  const minutes = parts.length > 1 ? parseInt(parts[1], 10) || 0 : 0;

  if (isPm && hours < 12) hours += 12;
  if (!isPm && hours === 12) hours = 0;

  return hours * 60 + minutes;
};

/**
 * Formats minutes of day into standard format like "5:30 PM"
 */
const formatMinutesToTimeStr = (totalMinutes) => {
  const hours24 = Math.floor(totalMinutes / 60) % 24;
  const mins = totalMinutes % 60;
  const isPm = hours24 >= 12;
  const hours12 = hours24 % 12 === 0 ? 12 : hours24 % 12;
  const paddedMins = mins.toString().padStart(2, '0');
  return `${hours12}:${paddedMins} ${isPm ? 'PM' : 'AM'}`;
};

/**
 * Real-Time Queue Engine enforcing all core business rules:
 * Rule 1: Barber-specific (do not count customers booked with another barber)
 * Rule 2: Relevant bookings only (CONFIRMED, CHECKED_IN, IN_PROGRESS, DELAYED)
 * Rule 3: Count customers scheduled before current customer's appointment time (plus in chair)
 * Rule 4: Service duration considered (sum of actual scheduled + remaining + delays)
 * Rule 5: Interactive mutations with event broadcasting
 */
class QueueService {
  /**
   * Calculates a customer's real-time queue position.
   */
  async calculateUserQueuePosition({ bookingId, barberName, scheduledTime, userId = null }) {
    const targetMinutes = parseTimeToMinutesOfDay(scheduledTime);

    // Rule 1: Barber-specific filter
    const barberItems = await BarberQueue.find({
      barberName: { $regex: new RegExp(`^${barberName.trim()}$`, 'i') },
    }).sort({ scheduledTimeMinutes: 1, createdAt: 1 });

    // Find current user's item if exists
    const currentItem = barberItems.find(
      (it) => (bookingId && it.bookingId === bookingId) || (userId && it.userId && it.userId.toString() === userId.toString()) || it.isCurrentUser
    );

    const effectiveTargetMinutes = currentItem ? currentItem.scheduledTimeMinutes : targetMinutes;

    // Rule 2: Relevant bookings only (CONFIRMED, CHECKED_IN, IN_PROGRESS, DELAYED)
    const relevantStatuses = ['CONFIRMED', 'CHECKED_IN', 'IN_PROGRESS', 'DELAYED'];
    const relevantItems = barberItems.filter((it) => relevantStatuses.includes(it.status));

    // Active customer in chair
    const activeCustomer = relevantItems.find((it) => it.status === 'IN_PROGRESS') || null;

    // Rule 3: Customers scheduled before the current customer's appointment
    const customersAhead = relevantItems.filter((item) => {
      const isCurrent =
        (bookingId && item.bookingId === bookingId) ||
        (currentItem && item._id.toString() === currentItem._id.toString());
      if (isCurrent) return false;

      if (item.status === 'IN_PROGRESS') {
        // Anyone currently in chair is ahead
        return true;
      } else {
        // Scheduled strictly before the current customer
        return item.scheduledTimeMinutes < effectiveTargetMinutes;
      }
    });

    // Rule 4: Service duration considered
    let totalWaitMinutes = 0;
    for (const ahead of customersAhead) {
      if (ahead.status === 'IN_PROGRESS') {
        totalWaitMinutes += Math.max(5, ahead.remainingMinutes);
      } else {
        totalWaitMinutes += ahead.durationMinutes + (ahead.delayMinutes || 0);
      }
    }

    const isYourTurn =
      customersAhead.length === 0 &&
      currentItem != null &&
      ['IN_PROGRESS', 'CHECKED_IN'].includes(currentItem.status);

    return {
      bookingId: bookingId || (currentItem ? currentItem.bookingId : 'BK_UNASSIGNED'),
      barberName,
      scheduledTime,
      customersBeforeCount: customersAhead.length,
      estimatedWaitingMinutes: totalWaitMinutes,
      activeCustomer,
      customersAhead,
      totalWaitingForBarber: relevantItems.length,
      isYourTurn,
      lastSyncTimestamp: Date.now(),
    };
  }

  /**
   * Get queue summaries across all barbers (active count & wait time)
   */
  async getBarberSummaries() {
    const allItems = await BarberQueue.find({
      status: { $in: ['CONFIRMED', 'CHECKED_IN', 'IN_PROGRESS', 'DELAYED'] },
    }).sort({ scheduledTimeMinutes: 1 });

    const grouped = {};
    for (const item of allItems) {
      if (!grouped[item.barberName]) {
        grouped[item.barberName] = [];
      }
      grouped[item.barberName].push(item);
    }

    const summaries = {};
    for (const [barberName, items] of Object.entries(grouped)) {
      let waitMins = 0;
      for (const it of items) {
        if (it.status === 'IN_PROGRESS') {
          waitMins += Math.max(5, it.remainingMinutes);
        } else {
          waitMins += it.durationMinutes + (it.delayMinutes || 0);
        }
      }

      summaries[barberName] = {
        barberId: items[0]?.barberId || 'b1',
        barberName,
        shopId: items[0]?.shopId || 'shop_1',
        activeWaitingCount: items.length,
        estimatedWaitMinutes: waitMins,
      };
    }

    return summaries;
  }

  /**
   * Rule 5 Mutation: Start service for a customer
   */
  async startBarberService(queueItemId) {
    const item = await BarberQueue.findOne({
      $or: [{ queueId: queueItemId }, { _id: queueItemId.match(/^[0-9a-fA-F]{24}$/) ? queueItemId : null }],
    });
    if (!item) return null;

    // Check if another customer is in chair, complete them first
    await BarberQueue.updateMany(
      { barberName: item.barberName, status: 'IN_PROGRESS', queueId: { $ne: item.queueId } },
      { status: 'COMPLETED', remainingMinutes: 0, completedAt: new Date() }
    );

    item.status = 'IN_PROGRESS';
    item.remainingMinutes = item.durationMinutes;
    item.startedAt = new Date();
    await item.save();

    broadcastQueueEvent(item.barberName, {
      eventType: 'SERVICE_STARTED',
      customerName: item.customerName,
      message: `✂️ Barber ${item.barberName} started service for ${item.customerName} (${item.durationMinutes}m)`,
      queueItemId: item.queueId,
    });

    return item;
  }

  /**
   * Rule 5 Mutation: Complete service for currently in-chair customer
   */
  async completeBarberService(barberName) {
    const active = await BarberQueue.findOne({
      barberName: { $regex: new RegExp(`^${barberName.trim()}$`, 'i') },
      status: 'IN_PROGRESS',
    });

    if (active) {
      active.status = 'COMPLETED';
      active.remainingMinutes = 0;
      active.completedAt = new Date();
      await active.save();

      // Check if next customer is checked in or confirmed and promote
      const nextCustomer = await BarberQueue.findOne({
        barberName: { $regex: new RegExp(`^${barberName.trim()}$`, 'i') },
        status: { $in: ['CHECKED_IN', 'CONFIRMED'] },
      }).sort({ scheduledTimeMinutes: 1 });

      broadcastQueueEvent(barberName, {
        eventType: 'SERVICE_COMPLETED',
        customerName: active.customerName,
        message: `✅ Service completed for ${active.customerName}. ${nextCustomer ? `Next up: ${nextCustomer.customerName}` : 'Chair is now free.'}`,
        nextCustomer: nextCustomer ? nextCustomer.customerName : null,
      });

      return { completedItem: active, nextCustomer };
    }

    return null;
  }

  /**
   * Rule 5 Mutation: Customer arrives and checks in
   */
  async checkInCustomer(queueItemId) {
    const item = await BarberQueue.findOne({
      $or: [{ queueId: queueItemId }, { _id: queueItemId.match(/^[0-9a-fA-F]{24}$/) ? queueItemId : null }],
    });
    if (!item) return null;

    item.status = 'CHECKED_IN';
    await item.save();

    broadcastQueueEvent(item.barberName, {
      eventType: 'CUSTOMER_CHECKED_IN',
      customerName: item.customerName,
      message: `📍 ${item.customerName} checked in at the shop and is waiting in lounge.`,
      queueItemId: item.queueId,
    });

    return item;
  }

  /**
   * Rule 5 Mutation: Add unexpected delay to a customer
   */
  async addCustomerDelay(queueItemId, delayMinutes = 15) {
    const item = await BarberQueue.findOne({
      $or: [{ queueId: queueItemId }, { _id: queueItemId.match(/^[0-9a-fA-F]{24}$/) ? queueItemId : null }],
    });
    if (!item) return null;

    item.delayMinutes = (item.delayMinutes || 0) + delayMinutes;
    item.status = 'DELAYED';
    await item.save();

    broadcastQueueEvent(item.barberName, {
      eventType: 'DELAY_ALERT',
      customerName: item.customerName,
      message: `⚠️ Service for ${item.customerName} delayed by +${delayMinutes} mins. Queue wait times recalibrated.`,
      delayMinutes,
    });

    return item;
  }

  /**
   * Rule 5 Mutation: Add Walk-in customer to queue
   */
  async addNewWalkIn({ shopId = 'shop_1', barberId = 'b1', barberName = 'Rahul Sharma', customerName, serviceNames = ['Express Buzz Cut'], durationMinutes = 20 }) {
    const now = new Date();
    const currentMins = now.getHours() * 60 + now.getMinutes();
    const scheduledTime = formatMinutesToTimeStr(currentMins);

    const queueId = `Q_WALKIN_${Date.now()}`;
    const walkInItem = new BarberQueue({
      queueId,
      barberId,
      barberName,
      shopId,
      customerName: customerName || `Walk-in Guest (${queueId.takeRight(4)})`,
      isCurrentUser: false,
      bookingId: `BK_WALK_${Date.now().toString().slice(-4)}`,
      serviceNames,
      durationMinutes,
      scheduledTime,
      scheduledTimeMinutes: currentMins,
      status: 'CHECKED_IN',
      remainingMinutes: durationMinutes,
      delayMinutes: 0,
    });

    await walkInItem.save();

    broadcastQueueEvent(barberName, {
      eventType: 'WALK_IN_ADDED',
      customerName: walkInItem.customerName,
      message: `🚶 Walk-in customer ${walkInItem.customerName} added to ${barberName}'s queue (${durationMinutes}m).`,
      queueItemId: queueId,
    });

    return walkInItem;
  }

  /**
   * Remove / Cancel queue item
   */
  async cancelQueueItem(queueItemId, reason = 'Customer cancelled') {
    const item = await BarberQueue.findOne({
      $or: [{ queueId: queueItemId }, { bookingId: queueItemId }],
    });
    if (!item) return null;

    item.status = 'CANCELLED';
    await item.save();

    broadcastQueueEvent(item.barberName, {
      eventType: 'BOOKING_CANCELLED',
      customerName: item.customerName,
      message: `❌ Appointment cancelled for ${item.customerName}. Wait time decreased for all customers behind.`,
      queueItemId: item.queueId,
    });

    return item;
  }
}

module.exports = new QueueService();
