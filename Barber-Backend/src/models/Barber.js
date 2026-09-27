const mongoose = require('mongoose');

const barberSchema = new mongoose.Schema(
  {
    barberId: {
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
      index: true,
    },
    rating: {
      type: Number,
      default: 4.8,
      min: 0,
      max: 5,
    },
    experienceYears: {
      type: Number,
      default: 5,
    },
    specialty: {
      type: String,
      default: 'Master Barber / Hair Specialist',
    },
    isAvailable: {
      type: Boolean,
      default: true,
    },
    photoUrl: {
      type: String,
      default: '',
    },
    phone: {
      type: String,
      default: '',
    },
  },
  {
    timestamps: true,
  }
);

module.exports = mongoose.model('Barber', barberSchema);
