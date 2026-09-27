const express = require('express');
const router = express.Router();
const shopController = require('../controllers/shopController');
const { optionalAuth, protect } = require('../middleware/authMiddleware');

router.get('/', optionalAuth, shopController.getAllShops);
router.get('/:id', optionalAuth, shopController.getShopById);
router.post('/:id/favorite', protect, shopController.toggleFavorite);
router.get('/:id/reviews', shopController.getShopReviews);
router.post('/:id/reviews', optionalAuth, shopController.addShopReview);
router.get('/:id/slots', shopController.getShopSlots);

module.exports = router;
