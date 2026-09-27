const mongoose = require('mongoose');

const slotReservationSchema = new mongoose.Schema(
  {
    reservationId: {
      type: String,
      required: true,
      unique: true,
      index: true,
    },
    shopId: {
      type: String,
      required: true,
      index: true,
    },
    barberName: {
      type: String,
      required: true,
    },
    slotTime: {
      type: String,
      required: true,
    },
    slotDate: {
      type: String,
      required: true,
    },
    userId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      default: null,
    },
    customerName: {
      type: String,
      default: 'Guest User',
    },
    expiresAt: {
      type: Date,
      required: true,
      index: { expires: 0 }, // MongoDB TTL auto-cleanup on expiry
    },
    status: {
      type: String,
      enum: ['HELD', 'CONFIRMED', 'RELEASED', 'EXPIRED'],
      default: 'HELD',
    },
  },
  {
    timestamps: true,
  }
);

// Compound index to prevent double-booking on same slot
slotReservationSchema.index({ shopId: 1, barberName: 1, slotDate: 1, slotTime: 1, status: 1 });

module.exports = mongoose.model('SlotReservation', slotReservationSchema);
