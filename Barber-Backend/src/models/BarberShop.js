const mongoose = require('mongoose');

const barberShopSchema = new mongoose.Schema(
  {
    shopId: {
      type: String,
      required: true,
      unique: true,
      index: true,
    },
    name: {
      type: String,
      required: true,
      trim: true,
      index: true,
    },
    rating: {
      type: Number,
      default: 4.8,
      min: 0,
      max: 5,
    },
    reviewCount: {
      type: Number,
      default: 0,
    },
    distanceKm: {
      type: Number,
      default: 1.2,
    },
    startingPrice: {
      type: Number,
      default: 200,
    },
    nextAvailableSlot: {
      type: String,
      default: 'Available Today',
    },
    isOpen: {
      type: Boolean,
      default: true,
    },
    address: {
      type: String,
      required: true,
    },
    city: {
      type: String,
      default: 'Noida, Uttar Pradesh',
    },
    location: {
      type: {
        type: String,
        enum: ['Point'],
        default: 'Point',
      },
      coordinates: {
        type: [Number], // [longitude, latitude]
        default: [77.4500, 28.6500],
      },
    },
    imageUrl: {
      type: String,
      default: '',
    },
    description: {
      type: String,
      default: '',
    },
    phone: {
      type: String,
      default: '+91 98112 34567',
    },
    operatingHours: {
      open: { type: String, default: '09:00 AM' },
      close: { type: String, default: '09:00 PM' },
    },
    services: [
      {
        id: String,
        name: String,
        category: String,
        price: Number,
        durationMin: Number,
        description: String,
      },
    ],
    barbers: [
      {
        id: String,
        name: String,
        rating: Number,
        experienceYears: Number,
        specialty: String,
        isAvailable: Boolean,
      },
    ],
    reviews: [
      {
        id: String,
        userName: String,
        rating: Number,
        comment: String,
        date: String,
        barberName: String,
      },
    ],
  },
  {
    timestamps: true,
    toJSON: { virtuals: true },
  }
);

// 2dsphere index for location queries
barberShopSchema.index({ location: '2dsphere' });

module.exports = mongoose.model('BarberShop', barberShopSchema);
