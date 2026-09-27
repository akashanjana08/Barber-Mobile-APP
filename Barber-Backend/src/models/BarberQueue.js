const mongoose = require('mongoose');

const barberQueueSchema = new mongoose.Schema(
  {
    queueId: {
      type: String,
      required: true,
      unique: true,
      index: true,
    },
    barberId: {
      type: String,
      default: 'b1',
    },
    barberName: {
      type: String,
      required: true,
      index: true,
    },
    shopId: {
      type: String,
      required: true,
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
    isCurrentUser: {
      type: Boolean,
      default: false,
    },
    userId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      default: null,
    },
    bookingId: {
      type: String,
      default: null,
      index: true,
    },
    serviceNames: {
      type: [String],
      default: [],
    },
    durationMinutes: {
      type: Number,
      required: true,
      default: 30,
    },
    scheduledTime: {
      type: String,
      required: true,
    },
    scheduledTimeMinutes: {
      type: Number,
      required: true,
    },
    status: {
      type: String,
      enum: [
        'CONFIRMED',
        'CHECKED_IN',
        'IN_PROGRESS',
        'COMPLETED',
        'CANCELLED',
        'NO_SHOW',
        'DELAYED',
        'EXPIRED',
      ],
      default: 'CONFIRMED',
      index: true,
    },
    remainingMinutes: {
      type: Number,
      required: true,
      default: 30,
    },
    delayMinutes: {
      type: Number,
      default: 0,
    },
    startedAt: {
      type: Date,
      default: null,
    },
    completedAt: {
      type: Date,
      default: null,
    },
  },
  {
    timestamps: true,
  }
);

// Method to check relevance according to the business rules:
// CONFIRMED, CHECKED_IN, IN_PROGRESS, DELAYED are relevant.
// COMPLETED, CANCELLED, NO_SHOW, EXPIRED are not relevant.
barberQueueSchema.methods.isRelevant = function () {
  return ['CONFIRMED', 'CHECKED_IN', 'IN_PROGRESS', 'DELAYED'].includes(this.status);
};

module.exports = mongoose.model('BarberQueue', barberQueueSchema);
