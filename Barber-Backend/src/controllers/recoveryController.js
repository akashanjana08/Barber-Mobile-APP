const SlotOffer = require('../models/SlotOffer');
const Booking = require('../models/Booking');
const recoveryService = require('../services/recoveryService');

// @desc    Get active flash slot offers (not expired and not claimed)
// @route   GET /api/v1/offers/active
exports.getActiveOffers = async (req, res, next) => {
  try {
    const offers = await SlotOffer.find({
      status: 'SENT',
      expiresAt: { $gt: new Date() },
    }).sort({ createdAt: -1 });

    res.status(200).json({
      success: true,
      count: offers.length,
      data: offers,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Get single offer by code or id
// @route   GET /api/v1/offers/:id
exports.getOfferById = async (req, res, next) => {
  try {
    const offer = await SlotOffer.findOne({
      $or: [{ offerCode: req.params.id }, { _id: req.params.id.match(/^[0-9a-fA-F]{24}$/) ? req.params.id : null }],
    });

    if (!offer) {
      return res.status(404).json({ success: false, message: 'Offer not found' });
    }

    res.status(200).json({
      success: true,
      data: offer,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Claim flash offer and convert to confirmed booking
// @route   POST /api/v1/offers/:id/claim
exports.claimOffer = async (req, res, next) => {
  try {
    const { paymentMethod = 'UPI (Flash Deal)' } = req.body;
    const userId = req.user?._id || null;
    const customerName = req.user?.name || 'Akash Sharma';

    const result = await recoveryService.claimSlotOffer({
      offerId: req.params.id,
      userId,
      customerName,
      paymentMethod,
    });

    res.status(200).json({
      success: true,
      message: 'Flash offer claimed successfully! Booking confirmed with discount.',
      data: result,
    });
  } catch (error) {
    res.status(400).json({
      success: false,
      message: error.message,
    });
  }
};

// @desc    Reject / Dismiss flash offer
// @route   POST /api/v1/offers/:id/reject
exports.rejectOffer = async (req, res, next) => {
  try {
    const offer = await SlotOffer.findOne({
      $or: [{ offerCode: req.params.id }, { _id: req.params.id.match(/^[0-9a-fA-F]{24}$/) ? req.params.id : null }],
    });

    if (!offer) {
      return res.status(404).json({ success: false, message: 'Offer not found' });
    }

    offer.status = 'REJECTED';
    await offer.save();

    res.status(200).json({
      success: true,
      message: 'Offer dismissed',
      data: offer,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Evaluate target candidate users for slot recovery
// @route   GET /api/v1/offers/candidates/evaluate
exports.evaluateCandidates = async (req, res, next) => {
  try {
    const { shopLat = 28.6500, shopLng = 77.4500 } = req.query;

    const candidates = await recoveryService.evaluateTargetCandidates({
      shopLat: parseFloat(shopLat),
      shopLng: parseFloat(shopLng),
    });

    res.status(200).json({
      success: true,
      count: candidates.length,
      data: candidates,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Simulate slot cancellation recovery
// @route   POST /api/v1/offers/trigger-simulation
exports.triggerSimulation = async (req, res, next) => {
  try {
    const mockBooking = {
      shopId: 'shop_1',
      shopName: 'Royal Barber Club & Lounge',
      shopAddress: 'Sector 18, Wave Mall Complex, Noida',
      serviceNames: ['Signature Royal Haircut'],
      barberName: 'Rahul Sharma',
      dateStr: 'Today, 25 Sep',
      timeSlot: '5:30 PM',
      price: 300,
      customerName: 'Simulated Customer',
    };

    const offer = await recoveryService.createSlotOfferFromBooking({
      booking: mockBooking,
      sourceType: 'CUSTOMER_CANCELLATION',
    });

    res.status(201).json({
      success: true,
      message: 'Simulation completed: Cancelled slot recovered into active 20% discount flash offer.',
      data: offer,
    });
  } catch (error) {
    next(error);
  }
};
