const mongoose = require('mongoose');

const notificationSchema = new mongoose.Schema(
  {
    notificationId: {
      type: String,
      required: true,
      unique: true,
      index: true,
    },
    userId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      default: null,
      index: true,
    },
    title: {
      type: String,
      required: true,
    },
    message: {
      type: String,
      required: true,
    },
    type: {
      type: String,
      enum: ['CONFIRMED', 'REMINDER', 'CANCELLED', 'RESCHEDULED', 'OFFER'],
      required: true,
      index: true,
    },
    isRead: {
      type: Boolean,
      default: false,
    },
    relatedBookingId: {
      type: String,
      default: null,
    },
    relatedOfferId: {
      type: String,
      default: null,
    },
  },
  {
    timestamps: true,
  }
);

module.exports = mongoose.model('Notification', notificationSchema);
