const express = require('express');
const router = express.Router();
const paymentController = require('../controllers/paymentController');

router.post('/create-order', paymentController.createOrder);
router.post('/verify', paymentController.verifyPayment);
router.post('/refund', paymentController.refund);
router.get('/config/third-party', paymentController.getThirdPartyStatus);

module.exports = router;
