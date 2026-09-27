const express = require('express');
const router = express.Router();
const queueController = require('../controllers/queueController');
const { optionalAuth } = require('../middleware/authMiddleware');

router.get('/stream', queueController.streamQueueEvents);
router.get('/summaries', queueController.getBarberSummaries);
router.get('/position', optionalAuth, queueController.getUserQueuePosition);
router.get('/barber/:barberName', queueController.getBarberQueue);

// Interactive mutations
router.post('/start', queueController.startService);
router.post('/complete', queueController.completeService);
router.post('/checkin', queueController.checkInCustomer);
router.post('/delay', queueController.addDelay);
router.post('/walkin', queueController.addWalkIn);
router.post('/cancel', queueController.cancelQueueItem);
router.post('/reset', queueController.resetBenchmarkQueue);

module.exports = router;
