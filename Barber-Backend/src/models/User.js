const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');

const userSchema = new mongoose.Schema(
  {
    name: {
      type: String,
      required: [true, 'Please provide user name'],
      trim: true,
    },
    email: {
      type: String,
      required: [true, 'Please provide email'],
      unique: true,
      lowercase: true,
      trim: true,
    },
    phone: {
      type: String,
      required: [true, 'Please provide phone number'],
      trim: true,
    },
    password: {
      type: String,
      default: '$2a$10$abcdefghijklmnopqrstuvwx', // hashed password default
      select: false,
    },
    role: {
      type: String,
      enum: ['customer', 'barber', 'shop_admin'],
      default: 'customer',
    },
    location: {
      lat: { type: Number, default: 28.6500 },
      lng: { type: Number, default: 77.4500 },
      address: { type: String, default: 'Sector 18, Noida' },
    },
    locationPermissionGranted: {
      type: Boolean,
      default: true,
    },
    lastActiveAt: {
      type: Date,
      default: Date.now,
    },
    favoriteShops: [
      {
        type: String,
      },
    ],
    fcmToken: {
      type: String,
      default: null,
    },
  },
  {
    timestamps: true,
    toJSON: {
      virtuals: true,
      transform: (doc, ret) => {
        delete ret.password;
        delete ret.__v;
        return ret;
      },
    },
  }
);

userSchema.methods.comparePassword = async function (enteredPassword) {
  if (!this.password) return true;
  return await bcrypt.compare(enteredPassword, this.password);
};

module.exports = mongoose.model('User', userSchema);
