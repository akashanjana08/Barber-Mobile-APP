const Booking = require('../models/Booking');
const SlotReservation = require('../models/SlotReservation');
const BarberQueue = require('../models/BarberQueue');
const Notification = require('../models/Notification');
const recoveryService = require('../services/recoveryService');
const { parseTimeToMinutesOfDay, cancelQueueItem } = require('../services/queueService');
const { paymentService, notificationService } = require('../services/thirdPartyService');
const { broadcastQueueEvent, emitToUser } = require('../services/socketService');
const config = require('../config/env');

// @desc    Hold slot temporarily (10-minute lock with 409 conflict detection)
// @route   POST /api/v1/bookings/hold
exports.holdSlot = async (req, res, next) => {
  try {
    const { shopId, barberName, slotTime, slotDate, simulateConflict } = req.body;

    if (!shopId || !barberName || !slotTime || !slotDate) {
      return res.status(400).json({
        success: false,
        message: 'shopId, barberName, slotTime, and slotDate are required',
      });
    }

    if (simulateConflict) {
      return res.status(409).json({
        success: false,
        message: 'HTTP 409 Conflict: This slot was just booked by another customer. Please select another slot.',
      });
    }

    // Check if slot is already actively held by someone else
    const existingHold = await SlotReservation.findOne({
      shopId,
      barberName,
      slotDate,
      slotTime,
      status: 'HELD',
      expiresAt: { $gt: new Date() },
    });

    if (existingHold && (!req.user || existingHold.userId?.toString() !== req.user._id.toString())) {
      return res.status(409).json({
        success: false,
        message: 'HTTP 409 Conflict: This slot is currently held by another user completing payment. Please choose another time slot.',
      });
    }

    const holdTimeoutMs = config.businessRules.slotHoldTimeoutMinutes * 60 * 1000;
    const expiresAt = new Date(Date.now() + holdTimeoutMs);
    const reservationId = `RES_${Date.now().toString().slice(-6)}_${Math.random().toString(36).substring(2, 5).toUpperCase()}`;

    const reservation = await SlotReservation.create({
      reservationId,
      shopId,
      barberName,
      slotTime,
      slotDate,
      userId: req.user?._id || null,
      customerName: req.user?.name || 'Customer',
      expiresAt,
      status: 'HELD',
    });

    res.status(200).json({
      success: true,
      message: `Slot held for ${config.businessRules.slotHoldTimeoutMinutes} minutes. Complete checkout to confirm.`,
      data: {
        reservationId: reservation.reservationId,
        shopId: reservation.shopId,
        slotTime: reservation.slotTime,
        slotDate: reservation.slotDate,
        expiresAt: reservation.expiresAt,
        expiresAtTimestamp: reservation.expiresAt.getTime(),
      },
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Create / Confirm new booking
// @route   POST /api/v1/bookings
exports.createBooking = async (req, res, next) => {
  try {
    const {
      reservationId,
      shopId,
      shopName,
      shopAddress,
      serviceNames,
      barberName,
      dateStr,
      timeSlot,
      price,
      paymentMethod = 'UPI (Google Pay)',
      durationMinutes = 45,
    } = req.body;

    if (!shopId || !barberName || !serviceNames || !dateStr || !timeSlot || price === undefined) {
      return res.status(400).json({
        success: false,
        message: 'Missing required booking details (shopId, barberName, serviceNames, dateStr, timeSlot, price)',
      });
    }

    // Process payment verification via Third-Party Payment Gateway
    const paymentResult = await paymentService.createOrder({
      amount: price,
      receipt: `bk_rcpt_${Date.now()}`,
    });

    const tax = Math.round(price * 0.05); // 5% GST
    const total = price + tax;
    const scheduledTimeMinutes = parseTimeToMinutesOfDay(timeSlot);
    const bookingNumber = `BK${Date.now().toString().slice(-5)}`;

    const booking = await Booking.create({
      bookingNumber,
      userId: req.user?._id || null,
      customerName: req.user?.name || 'Akash Sharma',
      customerPhone: req.user?.phone || '+91 98765 12340',
      shopId,
      shopName,
      shopAddress,
      serviceNames: Array.isArray(serviceNames) ? serviceNames : [serviceNames],
      barberName,
      dateStr,
      timeSlot,
      scheduledTimeMinutes,
      durationMinutes,
      price,
      tax,
      total,
      status: 'UPCOMING',
      paymentStatus: 'PAID',
      paymentMethod,
      transactionId: paymentResult.orderId,
    });

    // Mark slot reservation as CONFIRMED if present
    if (reservationId) {
      await SlotReservation.updateOne({ reservationId }, { status: 'CONFIRMED' });
    }

    // Insert into BarberQueue
    const queueItem = await BarberQueue.create({
      queueId: `Q_${bookingNumber}`,
      barberId: 'b1',
      barberName,
      shopId,
      customerName: booking.customerName,
      customerPhone: booking.customerPhone,
      isCurrentUser: true,
      userId: req.user?._id || null,
      bookingId: bookingNumber,
      serviceNames: booking.serviceNames,
      durationMinutes,
      scheduledTime: timeSlot,
      scheduledTimeMinutes,
      status: 'CONFIRMED',
      remainingMinutes: durationMinutes,
      delayMinutes: 0,
    });

    // Create Confirmation Notification
    const notif = await Notification.create({
      notificationId: `notif_bk_${Date.now()}`,
      userId: req.user?._id || null,
      title: 'Booking Confirmed!',
      message: `Your appointment at ${shopName} for ${booking.serviceNames.join(', ')} with ${barberName} on ${dateStr} at ${timeSlot} is confirmed!`,
      type: 'CONFIRMED',
      relatedBookingId: bookingNumber,
    });

    // Broadcast Real-time event
    broadcastQueueEvent(barberName, {
      eventType: 'NEW_BOOKING',
      customerName: booking.customerName,
      message: `📅 New appointment booked for ${booking.customerName} with ${barberName} at ${timeSlot}.`,
      bookingId: bookingNumber,
    });

    res.status(201).json({
      success: true,
      message: 'Booking confirmed successfully',
      data: booking,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Get user's bookings with optional status filter
// @route   GET /api/v1/bookings
exports.getUserBookings = async (req, res, next) => {
  try {
    const { status } = req.query;
    let query = {};

    if (req.user) {
      query.$or = [{ userId: req.user._id }, { customerName: req.user.name }];
    }

    if (status) {
      query.status = status.toUpperCase();
    }

    const bookings = await Booking.find(query).sort({ createdAt: -1 });

    res.status(200).json({
      success: true,
      count: bookings.length,
      data: bookings,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Get booking by ID
// @route   GET /api/v1/bookings/:id
exports.getBookingById = async (req, res, next) => {
  try {
    const booking = await Booking.findOne({
      $or: [{ bookingNumber: req.params.id }, { _id: req.params.id.match(/^[0-9a-fA-F]{24}$/) ? req.params.id : null }],
    });

    if (!booking) {
      return res.status(404).json({ success: false, message: 'Booking not found' });
    }

    res.status(200).json({
      success: true,
      data: booking,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Cancel booking (Triggers Smart Recovery / Slot Offer Generation)
// @route   POST /api/v1/bookings/:id/cancel
exports.cancelBooking = async (req, res, next) => {
  try {
    const { reason = 'Customer request' } = req.body;

    const booking = await Booking.findOne({
      $or: [{ bookingNumber: req.params.id }, { _id: req.params.id.match(/^[0-9a-fA-F]{24}$/) ? req.params.id : null }],
    });

    if (!booking) {
      return res.status(404).json({ success: false, message: 'Booking not found' });
    }

    if (booking.status === 'CANCELLED') {
      return res.status(400).json({ success: false, message: 'Booking is already cancelled' });
    }

    booking.status = 'CANCELLED';
    booking.cancellationReason = reason;
    await booking.save();

    // Remove from BarberQueue
    await BarberQueue.deleteOne({ bookingId: booking.bookingNumber });

    // Process refund via Payment Gateway
    if (booking.paymentStatus === 'PAID') {
      await paymentService.processRefund({
        transactionId: booking.transactionId || 'txn_sample',
        amount: booking.total,
        reason: 'Appointment cancellation',
      });
      booking.paymentStatus = 'REFUNDED';
      await booking.save();
    }

    // Trigger Smart Recovery: Create Slot Offer for Nearby Eligible Customers
    const recoveryOffer = await recoveryService.createSlotOfferFromBooking({
      booking,
      sourceType: 'CUSTOMER_CANCELLATION',
    });

    // Notify current user
    await Notification.create({
      notificationId: `notif_canc_${Date.now()}`,
      userId: booking.userId || req.user?._id,
      title: 'Booking Cancelled',
      message: `Your appointment ${booking.bookingNumber} at ${booking.shopName} for ${booking.timeSlot} has been cancelled. Refund has been initiated.`,
      type: 'CANCELLED',
      relatedBookingId: booking.bookingNumber,
    });

    res.status(200).json({
      success: true,
      message: 'Booking cancelled successfully. Slot has been added to Smart Recovery queue.',
      data: {
        booking,
        recoveryOffer,
      },
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Reschedule booking
// @route   POST /api/v1/bookings/:id/reschedule
exports.rescheduleBooking = async (req, res, next) => {
  try {
    const { newDateStr, newTimeSlot, newBarberName } = req.body;

    if (!newDateStr || !newTimeSlot) {
      return res.status(400).json({ success: false, message: 'newDateStr and newTimeSlot are required' });
    }

    const booking = await Booking.findOne({
      $or: [{ bookingNumber: req.params.id }, { _id: req.params.id.match(/^[0-9a-fA-F]{24}$/) ? req.params.id : null }],
    });

    if (!booking) {
      return res.status(404).json({ success: false, message: 'Booking not found' });
    }

    const oldBarber = booking.barberName;
    const oldTime = booking.timeSlot;

    booking.dateStr = newDateStr;
    booking.timeSlot = newTimeSlot;
    if (newBarberName) booking.barberName = newBarberName;
    booking.scheduledTimeMinutes = parseTimeToMinutesOfDay(newTimeSlot);
    booking.status = 'UPCOMING';
    booking.isLate = false;
    await booking.save();

    // Update queue item
    await BarberQueue.findOneAndUpdate(
      { bookingId: booking.bookingNumber },
      {
        barberName: booking.barberName,
        scheduledTime: newTimeSlot,
        scheduledTimeMinutes: booking.scheduledTimeMinutes,
        status: 'CONFIRMED',
        delayMinutes: 0,
      }
    );

    // Notification
    await Notification.create({
      notificationId: `notif_resched_${Date.now()}`,
      userId: booking.userId || req.user?._id,
      title: 'Appointment Rescheduled',
      message: `Your appointment has been rescheduled from ${oldTime} to ${newTimeSlot} on ${newDateStr} with ${booking.barberName}.`,
      type: 'RESCHEDULED',
      relatedBookingId: booking.bookingNumber,
    });

    broadcastQueueEvent(booking.barberName, {
      eventType: 'BOOKING_RESCHEDULED',
      customerName: booking.customerName,
      message: `🔄 ${booking.customerName} rescheduled from ${oldTime} to ${newTimeSlot}.`,
      bookingId: booking.bookingNumber,
    });

    res.status(200).json({
      success: true,
      message: 'Booking rescheduled successfully',
      data: booking,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Mark booking as No-Show
// @route   POST /api/v1/bookings/:id/no-show
exports.markBookingNoShow = async (req, res, next) => {
  try {
    const booking = await Booking.findOne({
      $or: [{ bookingNumber: req.params.id }, { _id: req.params.id.match(/^[0-9a-fA-F]{24}$/) ? req.params.id : null }],
    });

    if (!booking) {
      return res.status(404).json({ success: false, message: 'Booking not found' });
    }

    booking.status = 'NO_SHOW';
    await booking.save();

    // Update queue item to NO_SHOW
    await BarberQueue.findOneAndUpdate({ bookingId: booking.bookingNumber }, { status: 'NO_SHOW' });

    // Trigger No-Show Slot Recovery Offer
    const recoveryOffer = await recoveryService.createSlotOfferFromBooking({
      booking,
      sourceType: 'NO_SHOW_RECOVERY',
    });

    broadcastQueueEvent(booking.barberName, {
      eventType: 'NO_SHOW_RECOVERY',
      customerName: booking.customerName,
      message: `⚠️ Customer ${booking.customerName} marked No-Show. Slot released for recovery.`,
      bookingId: booking.bookingNumber,
    });

    res.status(200).json({
      success: true,
      message: 'Booking marked as No-Show. Recovery offer generated for nearby users.',
      data: {
        booking,
        recoveryOffer,
      },
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Trigger 15-minute appointment reminder
// @route   POST /api/v1/bookings/:id/reminder
exports.sendAppointmentReminder = async (req, res, next) => {
  try {
    const booking = await Booking.findOne({
      $or: [{ bookingNumber: req.params.id }, { _id: req.params.id.match(/^[0-9a-fA-F]{24}$/) ? req.params.id : null }],
    });

    if (!booking) {
      return res.status(404).json({ success: false, message: 'Booking not found' });
    }

    booking.reminderSentAt = new Date();
    await booking.save();

    const notifMessage = `Your appointment at ${booking.shopName} starts in 15 minutes.\n\nService: ${booking.serviceNames.join(', ')}\nBarber: ${booking.barberName}\nTime: ${booking.timeSlot}\n\nPlease reach the barber shop before your scheduled time.`;

    const notif = await Notification.create({
      notificationId: `notif_rem_${Date.now()}`,
      userId: booking.userId || req.user?._id,
      title: '⏰ Appointment Reminder',
      message: notifMessage,
      type: 'REMINDER',
      relatedBookingId: booking.bookingNumber,
    });

    // Send SMS via Third-Party SMS provider
    await notificationService.sendSms({
      to: booking.customerPhone || '+91 98765 12340',
      message: `[BarberCraft] Reminder: Your appointment at ${booking.shopName} with ${booking.barberName} is in 15 mins (${booking.timeSlot}).`,
    });

    res.status(200).json({
      success: true,
      message: '15-minute appointment reminder sent successfully via In-App notification and SMS.',
      data: {
        booking,
        notification: notif,
      },
    });
  } catch (error) {
    next(error);
  }
};
