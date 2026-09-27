const BarberQueue = require('../models/BarberQueue');
const queueService = require('../services/queueService');
const { registerSseClient } = require('../services/socketService');

// @desc    Get queue items for a specific barber
// @route   GET /api/v1/queue/barber/:barberName
exports.getBarberQueue = async (req, res, next) => {
  try {
    const { barberName } = req.params;
    const items = await BarberQueue.find({
      barberName: { $regex: new RegExp(`^${barberName.trim()}$`, 'i') },
      status: { $in: ['CONFIRMED', 'CHECKED_IN', 'IN_PROGRESS', 'DELAYED'] },
    }).sort({ scheduledTimeMinutes: 1 });

    res.status(200).json({
      success: true,
      count: items.length,
      barberName,
      data: items,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Get user's real-time queue position with all business rules evaluated
// @route   GET /api/v1/queue/position
exports.getUserQueuePosition = async (req, res, next) => {
  try {
    const { bookingId, barberName, scheduledTime } = req.query;

    if (!barberName || !scheduledTime) {
      return res.status(400).json({
        success: false,
        message: 'barberName and scheduledTime query params are required',
      });
    }

    const position = await queueService.calculateUserQueuePosition({
      bookingId: bookingId || 'BK10234',
      barberName,
      scheduledTime,
      userId: req.user?._id || null,
    });

    res.status(200).json({
      success: true,
      data: position,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Get summaries for all barbers (active count & wait minutes)
// @route   GET /api/v1/queue/summaries
exports.getBarberSummaries = async (req, res, next) => {
  try {
    const summaries = await queueService.getBarberSummaries();
    res.status(200).json({
      success: true,
      data: summaries,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Start service for a customer in queue (Rule 5)
// @route   POST /api/v1/queue/start
exports.startService = async (req, res, next) => {
  try {
    const { queueItemId } = req.body;
    if (!queueItemId) {
      return res.status(400).json({ success: false, message: 'queueItemId is required' });
    }

    const updated = await queueService.startBarberService(queueItemId);
    if (!updated) {
      return res.status(404).json({ success: false, message: 'Queue item not found' });
    }

    res.status(200).json({
      success: true,
      message: `Service started for ${updated.customerName}. Status updated to IN_PROGRESS.`,
      data: updated,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Complete service for active customer in chair (Rule 5)
// @route   POST /api/v1/queue/complete
exports.completeService = async (req, res, next) => {
  try {
    const { barberName } = req.body;
    if (!barberName) {
      return res.status(400).json({ success: false, message: 'barberName is required' });
    }

    const result = await queueService.completeBarberService(barberName);
    if (!result) {
      return res.status(404).json({
        success: false,
        message: `No active customer in chair (IN_PROGRESS) for barber ${barberName}.`,
      });
    }

    res.status(200).json({
      success: true,
      message: `Service completed for ${result.completedItem.customerName}.`,
      data: result,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Check-in customer arrival (Rule 5)
// @route   POST /api/v1/queue/checkin
exports.checkInCustomer = async (req, res, next) => {
  try {
    const { queueItemId } = req.body;
    if (!queueItemId) {
      return res.status(400).json({ success: false, message: 'queueItemId is required' });
    }

    const updated = await queueService.checkInCustomer(queueItemId);
    if (!updated) {
      return res.status(404).json({ success: false, message: 'Queue item not found' });
    }

    res.status(200).json({
      success: true,
      message: `${updated.customerName} checked in.`,
      data: updated,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Add delay to customer service (Rule 5)
// @route   POST /api/v1/queue/delay
exports.addDelay = async (req, res, next) => {
  try {
    const { queueItemId, delayMinutes = 15 } = req.body;
    if (!queueItemId) {
      return res.status(400).json({ success: false, message: 'queueItemId is required' });
    }

    const updated = await queueService.addCustomerDelay(queueItemId, parseInt(delayMinutes, 10));
    if (!updated) {
      return res.status(404).json({ success: false, message: 'Queue item not found' });
    }

    res.status(200).json({
      success: true,
      message: `Added +${delayMinutes} mins delay to ${updated.customerName}.`,
      data: updated,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Add walk-in customer into queue (Rule 5)
// @route   POST /api/v1/queue/walkin
exports.addWalkIn = async (req, res, next) => {
  try {
    const { shopId, barberId, barberName, customerName, serviceNames, durationMinutes } = req.body;

    const item = await queueService.addNewWalkIn({
      shopId: shopId || 'shop_1',
      barberId: barberId || 'b1',
      barberName: barberName || 'Rahul Sharma',
      customerName: customerName || 'Walk-in Guest',
      serviceNames: serviceNames || ['Express Haircut & Shave'],
      durationMinutes: durationMinutes ? parseInt(durationMinutes, 10) : 20,
    });

    res.status(201).json({
      success: true,
      message: `Walk-in customer ${item.customerName} added to ${item.barberName}'s queue.`,
      data: item,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Cancel queue item
// @route   POST /api/v1/queue/cancel
exports.cancelQueueItem = async (req, res, next) => {
  try {
    const { queueItemId, reason } = req.body;
    if (!queueItemId) {
      return res.status(400).json({ success: false, message: 'queueItemId is required' });
    }

    const item = await queueService.cancelQueueItem(queueItemId, reason);
    if (!item) {
      return res.status(404).json({ success: false, message: 'Queue item not found' });
    }

    res.status(200).json({
      success: true,
      message: `Queue item cancelled. Wait times recalibrated.`,
      data: item,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Reset queue items to standard benchmark test state
// @route   POST /api/v1/queue/reset
exports.resetBenchmarkQueue = async (req, res, next) => {
  try {
    await BarberQueue.deleteMany({});

    const benchmarkItems = [
      {
        queueId: 'Q_RAHUL_1',
        barberId: 'b1',
        barberName: 'Rahul Sharma',
        shopId: 'shop_1',
        customerName: 'Customer A (Vikram M.)',
        isCurrentUser: false,
        bookingId: 'BK_EXT_01',
        serviceNames: ['Signature Royal Haircut'],
        durationMinutes: 30,
        scheduledTime: '5:00 PM',
        scheduledTimeMinutes: 1020,
        status: 'IN_PROGRESS',
        remainingMinutes: 15,
        delayMinutes: 0,
      },
      {
        queueId: 'Q_RAHUL_2',
        barberId: 'b1',
        barberName: 'Rahul Sharma',
        shopId: 'shop_1',
        customerName: 'Customer B (Siddharth J.)',
        isCurrentUser: false,
        bookingId: 'BK_EXT_02',
        serviceNames: ['Royal Beard Sculpt & Trim'],
        durationMinutes: 15,
        scheduledTime: '5:15 PM',
        scheduledTimeMinutes: 1035,
        status: 'CHECKED_IN',
        remainingMinutes: 15,
        delayMinutes: 0,
      },
      {
        queueId: 'Q_RAHUL_CURRENT',
        barberId: 'b1',
        barberName: 'Rahul Sharma',
        shopId: 'shop_1',
        customerName: 'You (Akash Sharma)',
        isCurrentUser: true,
        bookingId: 'BK10234',
        serviceNames: ['Signature Royal Haircut', 'Royal Beard Sculpt & Trim'],
        durationMinutes: 45,
        scheduledTime: '5:30 PM',
        scheduledTimeMinutes: 1050,
        status: 'CONFIRMED',
        remainingMinutes: 45,
        delayMinutes: 0,
      },
      {
        queueId: 'Q_RAHUL_3',
        barberId: 'b1',
        barberName: 'Rahul Sharma',
        shopId: 'shop_1',
        customerName: 'Customer C (Rohan G.)',
        isCurrentUser: false,
        bookingId: 'BK_EXT_03',
        serviceNames: ['Signature Royal Haircut'],
        durationMinutes: 30,
        scheduledTime: '5:45 PM',
        scheduledTimeMinutes: 1065,
        status: 'CONFIRMED',
        remainingMinutes: 30,
        delayMinutes: 0,
      },
      {
        queueId: 'Q_AMIT_1',
        barberId: 'b2',
        barberName: 'Amit Verma',
        shopId: 'shop_1',
        customerName: 'Customer D (Nitin K.)',
        isCurrentUser: false,
        bookingId: 'BK_EXT_04',
        serviceNames: ['Skin Fade & Edge-up'],
        durationMinutes: 15,
        scheduledTime: '5:00 PM',
        scheduledTimeMinutes: 1020,
        status: 'CONFIRMED',
        remainingMinutes: 15,
        delayMinutes: 0,
      },
    ];

    await BarberQueue.insertMany(benchmarkItems);

    res.status(200).json({
      success: true,
      message: 'Queue benchmark data reset successfully.',
      count: benchmarkItems.length,
      data: benchmarkItems,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Live SSE stream endpoint for real-time queue events
// @route   GET /api/v1/queue/stream
exports.streamQueueEvents = (req, res) => {
  registerSseClient(req, res);
};
