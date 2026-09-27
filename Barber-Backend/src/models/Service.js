const mongoose = require('mongoose');

const serviceSchema = new mongoose.Schema(
  {
    serviceId: {
      type: String,
      required: true,
      unique: true,
    },
    shopId: {
      type: String,
      required: true,
      index: true,
    },
    name: {
      type: String,
      required: true,
      trim: true,
    },
    category: {
      type: String,
      required: true,
      enum: ['Haircut', 'Beard', 'Hair Color', 'Facial', 'Hair Spa', 'Kids Haircut', 'Combo'],
      index: true,
    },
    price: {
      type: Number,
      required: true,
      min: 0,
    },
    durationMin: {
      type: Number,
      required: true,
      default: 30,
    },
    description: {
      type: String,
      default: '',
    },
    isPopular: {
      type: Boolean,
      default: false,
    },
  },
  {
    timestamps: true,
  }
);

module.exports = mongoose.model('Service', serviceSchema);
