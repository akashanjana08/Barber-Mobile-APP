const mongoose = require('mongoose');

const slotOfferSchema = new mongoose.Schema(
  {
    offerCode: {
      type: String,
      required: true,
      unique: true,
      index: true,
    },
    slotId: {
      type: String,
      required: true,
      index: true,
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
    shopRating: {
      type: Number,
      default: 4.8,
    },
    shopLat: {
      type: Number,
      default: 28.6500,
    },
    shopLng: {
      type: Number,
      default: 77.4500,
    },
    serviceName: {
      type: String,
      required: true,
    },
    barberName: {
      type: String,
      required: true,
    },
    dateStr: {
      type: String,
      required: true,
    },
    timeSlot: {
      type: String,
      required: true,
    },
    originalPrice: {
      type: Number,
      required: true,
    },
    discountPercent: {
      type: Number,
      default: 20,
    },
    offerPrice: {
      type: Number,
      required: true,
    },
    expiresAt: {
      type: Date,
      required: true,
      index: true,
    },
    bufferMinutes: {
      type: Number,
      default: 15,
    },
    status: {
      type: String,
      enum: [
        'CREATED',
        'SENT',
        'VIEWED',
        'ACCEPTED',
        'BOOKING_CREATED',
        'EXPIRED',
        'REJECTED',
      ],
      default: 'SENT',
      index: true,
    },
    sourceType: {
      type: String,
      enum: ['CUSTOMER_CANCELLATION', 'NO_SHOW_RECOVERY'],
      default: 'CUSTOMER_CANCELLATION',
    },
    distanceKm: {
      type: Number,
      default: 1.2,
    },
    reservedByUserId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      default: null,
    },
    targetCustomers: [
      {
        id: String,
        name: String,
        distanceKm: Number,
        lastActiveHoursAgo: Number,
        locationPermissionGranted: Boolean,
        isEligible: Boolean,
        ineligibilityReason: String,
      },
    ],
  },
  {
    timestamps: true,
  }
);

module.exports = mongoose.model('SlotOffer', slotOfferSchema);
