const express = require('express');
const router = express.Router();
const notificationController = require('../controllers/notificationController');
const { optionalAuth } = require('../middleware/authMiddleware');

router.get('/', optionalAuth, notificationController.getUserNotifications);
router.put('/read-all', optionalAuth, notificationController.markAllAsRead);
router.put('/:id/read', notificationController.markAsRead);
router.delete('/:id', notificationController.deleteNotification);

module.exports = router;
