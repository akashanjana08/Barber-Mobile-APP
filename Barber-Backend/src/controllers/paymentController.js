const { paymentService } = require('../services/thirdPartyService');
const config = require('../config/env');

// @desc    Create payment order with configured payment gateway (Razorpay / Stripe / Mock)
// @route   POST /api/v1/payments/create-order
exports.createOrder = async (req, res, next) => {
  try {
    const { amount, currency = 'INR', receipt, notes } = req.body;

    if (!amount || amount <= 0) {
      return res.status(400).json({ success: false, message: 'Valid payment amount is required' });
    }

    const order = await paymentService.createOrder({
      amount: parseInt(amount, 10),
      currency,
      receipt: receipt || `rcpt_${Date.now()}`,
      notes: notes || {},
    });

    res.status(200).json({
      success: true,
      message: 'Payment order created',
      data: order,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Verify payment transaction
// @route   POST /api/v1/payments/verify
exports.verifyPayment = async (req, res, next) => {
  try {
    const { orderId, paymentId, signature } = req.body;

    const verification = await paymentService.verifyPayment({
      orderId,
      paymentId,
      signature,
    });

    res.status(200).json({
      success: true,
      message: 'Payment verified successfully',
      data: verification,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Process refund
// @route   POST /api/v1/payments/refund
exports.refund = async (req, res, next) => {
  try {
    const { transactionId, amount, reason } = req.body;

    if (!transactionId || !amount) {
      return res.status(400).json({ success: false, message: 'transactionId and amount are required' });
    }

    const result = await paymentService.processRefund({
      transactionId,
      amount: parseInt(amount, 10),
      reason: reason || 'Customer requested refund',
    });

    res.status(200).json({
      success: true,
      message: 'Refund processed',
      data: result,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Get configured third-party resources info and diagnostics
// @route   GET /api/v1/config/third-party
exports.getThirdPartyStatus = (req, res) => {
  const tp = config.thirdParty;

  res.status(200).json({
    success: true,
    data: {
      payment: {
        provider: tp.payment.provider,
        currency: tp.payment.currency,
        isConfigured: tp.payment.provider !== 'mock' || true,
      },
      notifications: {
        provider: tp.notifications.provider,
        isConfigured: tp.notifications.provider !== 'mock' || true,
      },
      maps: {
        provider: tp.maps.provider,
        isConfigured: tp.maps.provider !== 'mock' || true,
        defaultCoordinates: {
          lat: tp.maps.defaultLat,
          lng: tp.maps.defaultLng,
        },
      },
      email: {
        provider: tp.email.provider,
        from: tp.email.from,
      },
      businessRules: config.businessRules,
    },
  });
};
