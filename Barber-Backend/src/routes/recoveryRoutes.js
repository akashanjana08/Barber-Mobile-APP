const express = require('express');
const router = express.Router();
const recoveryController = require('../controllers/recoveryController');
const { optionalAuth } = require('../middleware/authMiddleware');

router.get('/active', recoveryController.getActiveOffers);
router.get('/candidates/evaluate', recoveryController.evaluateCandidates);
router.get('/:id', recoveryController.getOfferById);
router.post('/:id/claim', optionalAuth, recoveryController.claimOffer);
router.post('/:id/reject', recoveryController.rejectOffer);
router.post('/trigger-simulation', recoveryController.triggerSimulation);

module.exports = router;
