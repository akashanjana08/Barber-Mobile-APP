const SlotOffer = require('../models/SlotOffer');
const Notification = require('../models/Notification');
const User = require('../models/User');
const Booking = require('../models/Booking');
const BarberQueue = require('../models/BarberQueue');
const { mapsService, notificationService } = require('./thirdPartyService');
const { broadcastQueueEvent, emitToUser } = require('./socketService');
const config = require('../config/env');

class RecoveryService {
  /**
   * Evaluates candidates for a recovered slot based on geolocation and recent activity.
   */
  async evaluateTargetCandidates({ shopLat, shopLng }) {
    const users = await User.find({ role: 'customer' });
    const candidates = [];

    for (const u of users) {
      const distanceKm = mapsService.calculateHaversineDistance(
        shopLat,
        shopLng,
        u.location?.lat || 28.6500,
        u.location?.lng || 77.4500
      );

      const hoursAgo = Math.max(
        0.5,
        parseFloat(((Date.now() - new Date(u.lastActiveAt || Date.now()).getTime()) / 3600000).toFixed(1))
      );

      let isEligible = true;
      let reason = '';

      if (distanceKm > 5.0) {
        isEligible = false;
        reason = `Distance (${distanceKm} km) exceeds 5.0 km radius`;
      } else if (hoursAgo > 48.0) {
        isEligible = false;
        reason = `Inactive for ${hoursAgo} hrs (limit 48 hrs)`;
      } else if (!u.locationPermissionGranted) {
        isEligible = false;
        reason = 'Location permission disabled by user';
      }

      candidates.push({
        id: u._id.toString(),
        name: u.name,
        distanceKm,
        lastActiveHoursAgo: hoursAgo,
        locationPermissionGranted: u.locationPermissionGranted !== false,
        isEligible,
        ineligibilityReason: reason,
      });
    }

    return candidates.sort((a, b) => (b.isEligible ? 1 : 0) - (a.isEligible ? 1 : 0) || a.distanceKm - b.distanceKm);
  }

  /**
   * Generates a recovered flash slot offer when a booking is cancelled or marked no-show.
   */
  async createSlotOfferFromBooking({ booking, sourceType = 'CUSTOMER_CANCELLATION' }) {
    const discountPercent = config.businessRules.slotRecoveryDiscountPercent;
    const offerPrice = Math.round(booking.price * (1 - discountPercent / 100));
    const offerCode = `OFFER_${Date.now().toString().slice(-6)}`;
    const slotId = `SLOT_${booking.shopId}_${booking.timeSlot.replace(/[\s:]/g, '')}`;

    // Evaluate eligible customers nearby
    const candidates = await this.evaluateTargetCandidates({
      shopLat: 28.6500,
      shopLng: 77.4500,
    });

    const expiresAt = new Date(Date.now() + 35 * 60 * 1000); // 35 minutes expiry

    const offer = new SlotOffer({
      offerCode,
      slotId,
      shopId: booking.shopId,
      shopName: booking.shopName,
      shopAddress: booking.shopAddress,
      shopRating: 4.8,
      shopLat: 28.6500,
      shopLng: 77.4500,
      serviceName: booking.serviceNames[0] || 'Signature Grooming',
      barberName: booking.barberName,
      dateStr: booking.dateStr,
      timeSlot: booking.timeSlot,
      originalPrice: booking.price,
      discountPercent,
      offerPrice,
      expiresAt,
      bufferMinutes: 15,
      status: 'SENT',
      sourceType,
      distanceKm: 1.2,
      targetCustomers: candidates,
    });

    await offer.save();

    // Create In-App Notification & Push Notification for target users
    const eligibleUsers = candidates.filter((c) => c.isEligible);
    for (const target of eligibleUsers) {
      const notif = new Notification({
        notificationId: `notif_offer_${Date.now()}_${target.id.slice(-4)}`,
        userId: target.id,
        title: '🔥 Last-Minute Barber Offer',
        message: `A slot just opened near you!\n\n${offer.shopName} ⭐ 4.8\n${offer.serviceName}\n₹${offer.originalPrice} → ₹${offer.offerPrice} (${offer.discountPercent}% OFF)\n${offer.dateStr} ${offer.timeSlot}\n📍 ${offer.distanceKm} km away\n\nClaim now before it expires!`,
        type: 'OFFER',
        relatedOfferId: offer.offerCode,
      });
      await notif.save();

      // Emit push and socket event
      emitToUser(target.id, 'new_offer', offer);
      await notificationService.sendPushNotification({
        fcmToken: null,
        title: notif.title,
        body: notif.message,
      });
    }

    // Broadcast queue event so queue waiting times adjust
    broadcastQueueEvent(booking.barberName, {
      eventType: 'SLOT_RECOVERED',
      customerName: booking.customerName,
      message: `⚡ Cancelled slot at ${booking.timeSlot} converted to Flash Offer (${discountPercent}% OFF).`,
      offerCode,
    });

    return offer;
  }

  /**
   * Claim and book a flash slot offer
   */
  async claimSlotOffer({ offerId, userId, customerName, paymentMethod = 'UPI (Flash Deal)' }) {
    const offer = await SlotOffer.findOne({
      $or: [{ offerCode: offerId }, { _id: offerId.match(/^[0-9a-fA-F]{24}$/) ? offerId : null }],
    });

    if (!offer) {
      throw new Error('Offer not found');
    }

    if (offer.status !== 'SENT') {
      throw new Error(`Offer is no longer available (Status: ${offer.status})`);
    }

    if (new Date() > new Date(offer.expiresAt)) {
      offer.status = 'EXPIRED';
      await offer.save();
      throw new Error('This flash offer has expired.');
    }

    // Mark offer as accepted
    offer.status = 'BOOKING_CREATED';
    offer.reservedByUserId = userId;
    await offer.save();

    // Create the booking
    const bookingNumber = `BK${Date.now().toString().slice(-5)}`;
    const tax = Math.round(offer.offerPrice * 0.05);
    const total = offer.offerPrice + tax;

    const newBooking = new Booking({
      bookingNumber,
      userId,
      customerName: customerName || 'Valued Customer',
      shopId: offer.shopId,
      shopName: offer.shopName,
      shopAddress: offer.shopAddress,
      serviceNames: [offer.serviceName],
      barberName: offer.barberName,
      dateStr: offer.dateStr,
      timeSlot: offer.timeSlot,
      scheduledTimeMinutes: 1050, // 5:30 PM default
      durationMinutes: 30,
      price: offer.offerPrice,
      tax,
      total,
      status: 'UPCOMING',
      paymentStatus: 'PAID',
      paymentMethod,
      recoveredOfferId: offer.offerCode,
    });

    await newBooking.save();

    // Insert back into BarberQueue
    const queueItem = new BarberQueue({
      queueId: `Q_${bookingNumber}`,
      barberId: 'b1',
      barberName: offer.barberName,
      shopId: offer.shopId,
      customerName: newBooking.customerName,
      isCurrentUser: true,
      userId,
      bookingId: bookingNumber,
      serviceNames: [offer.serviceName],
      durationMinutes: 30,
      scheduledTime: offer.timeSlot,
      scheduledTimeMinutes: 1050,
      status: 'CONFIRMED',
      remainingMinutes: 30,
      delayMinutes: 0,
    });
    await queueItem.save();

    // Confirmation notification
    const notif = new Notification({
      notificationId: `notif_conf_${Date.now()}`,
      userId,
      title: '🎉 Flash Offer Claimed & Confirmed!',
      message: `Your booking at ${offer.shopName} for ${offer.serviceName} at ${offer.timeSlot} is confirmed with ${offer.discountPercent}% discount!`,
      type: 'CONFIRMED',
      relatedBookingId: bookingNumber,
    });
    await notif.save();

    broadcastQueueEvent(offer.barberName, {
      eventType: 'OFFER_CLAIMED',
      customerName: newBooking.customerName,
      message: `🎯 ${newBooking.customerName} claimed flash slot for ${offer.timeSlot}.`,
      bookingId: bookingNumber,
    });

    return { booking: newBooking, offer };
  }
}

module.exports = new RecoveryService();
