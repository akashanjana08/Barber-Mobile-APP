const express = require('express');
const router = express.Router();

const authRoutes = require('./authRoutes');
const shopRoutes = require('./shopRoutes');
const bookingRoutes = require('./bookingRoutes');
const queueRoutes = require('./queueRoutes');
const recoveryRoutes = require('./recoveryRoutes');
const notificationRoutes = require('./notificationRoutes');
const paymentRoutes = require('./paymentRoutes');
const { getDBStatus } = require('../config/db');

// Root API Health and Info
router.get('/health', (req, res) => {
  res.status(200).json({
    status: 'online',
    timestamp: new Date(),
    service: 'BarberCraft Express API',
    database: getDBStatus(),
  });
});

// Mount Resource Routes
router.use('/auth', authRoutes);
router.use('/shops', shopRoutes);
router.use('/bookings', bookingRoutes);
router.use('/queue', queueRoutes);
router.use('/offers', recoveryRoutes);
router.use('/notifications', notificationRoutes);
router.use('/payments', paymentRoutes);

module.exports = router;
