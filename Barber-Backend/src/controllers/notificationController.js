const Notification = require('../models/Notification');

// @desc    Get current user's notifications
// @route   GET /api/v1/notifications
exports.getUserNotifications = async (req, res, next) => {
  try {
    let query = {};
    if (req.user) {
      query.$or = [{ userId: req.user._id }, { userId: null }];
    }

    const notifications = await Notification.find(query).sort({ createdAt: -1 });

    res.status(200).json({
      success: true,
      count: notifications.length,
      data: notifications,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Mark notification as read
// @route   PUT /api/v1/notifications/:id/read
exports.markAsRead = async (req, res, next) => {
  try {
    const notif = await Notification.findOne({
      $or: [{ notificationId: req.params.id }, { _id: req.params.id.match(/^[0-9a-fA-F]{24}$/) ? req.params.id : null }],
    });

    if (!notif) {
      return res.status(404).json({ success: false, message: 'Notification not found' });
    }

    notif.isRead = true;
    await notif.save();

    res.status(200).json({
      success: true,
      message: 'Notification marked as read',
      data: notif,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Mark all notifications as read
// @route   PUT /api/v1/notifications/read-all
exports.markAllAsRead = async (req, res, next) => {
  try {
    let query = {};
    if (req.user) {
      query.$or = [{ userId: req.user._id }, { userId: null }];
    }

    await Notification.updateMany(query, { isRead: true });

    res.status(200).json({
      success: true,
      message: 'All notifications marked as read',
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Delete notification
// @route   DELETE /api/v1/notifications/:id
exports.deleteNotification = async (req, res, next) => {
  try {
    const notif = await Notification.findOneAndDelete({
      $or: [{ notificationId: req.params.id }, { _id: req.params.id.match(/^[0-9a-fA-F]{24}$/) ? req.params.id : null }],
    });

    if (!notif) {
      return res.status(404).json({ success: false, message: 'Notification not found' });
    }

    res.status(200).json({
      success: true,
      message: 'Notification deleted',
    });
  } catch (error) {
    next(error);
  }
};
