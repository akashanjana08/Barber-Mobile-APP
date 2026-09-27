const mongoose = require('mongoose');

const bookingSchema = new mongoose.Schema(
  {
    bookingNumber: {
      type: String,
      required: true,
      unique: true,
      index: true,
    },
    userId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      index: true,
    },
    customerName: {
      type: String,
      required: true,
    },
    customerPhone: {
      type: String,
      default: '',
    },
    shopId: {
      type: String,
      required: true,
      index: true,
    },
    shopName: {
      type: String,
      required: true,
    },
    shopAddress: {
      type: String,
      required: true,
    },
    serviceNames: {
      type: [String],
      required: true,
    },
    barberName: {
      type: String,
      required: true,
      index: true,
    },
    dateStr: {
      type: String,
      required: true,
      index: true,
    },
    timeSlot: {
      type: String,
      required: true,
    },
    scheduledTimeMinutes: {
      type: Number,
      required: true,
    },
    durationMinutes: {
      type: Number,
      default: 45,
    },
    price: {
      type: Number,
      required: true,
    },
    tax: {
      type: Number,
      default: 0,
    },
    total: {
      type: Number,
      required: true,
    },
    status: {
      type: String,
      enum: ['UPCOMING', 'COMPLETED', 'CANCELLED', 'NO_SHOW'],
      default: 'UPCOMING',
      index: true,
    },
    paymentStatus: {
      type: String,
      enum: ['PAID', 'PENDING', 'REFUNDED'],
      default: 'PAID',
    },
    paymentMethod: {
      type: String,
      default: 'UPI (Google Pay)',
    },
    transactionId: {
      type: String,
      default: '',
    },
    reminderSentAt: {
      type: Date,
      default: null,
    },
    isLate: {
      type: Boolean,
      default: false,
    },
    cancellationReason: {
      type: String,
      default: '',
    },
    recoveredOfferId: {
      type: String,
      default: null,
    },
  },
  {
    timestamps: true,
  }
);

module.exports = mongoose.model('Booking', bookingSchema);
