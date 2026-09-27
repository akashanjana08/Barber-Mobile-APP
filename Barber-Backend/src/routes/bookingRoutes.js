const express = require('express');
const router = express.Router();
const bookingController = require('../controllers/bookingController');
const { optionalAuth, protect } = require('../middleware/authMiddleware');

router.post('/hold', optionalAuth, bookingController.holdSlot);
router.post('/', optionalAuth, bookingController.createBooking);
router.get('/', optionalAuth, bookingController.getUserBookings);
router.get('/:id', optionalAuth, bookingController.getBookingById);
router.post('/:id/cancel', optionalAuth, bookingController.cancelBooking);
router.post('/:id/reschedule', optionalAuth, bookingController.rescheduleBooking);
router.post('/:id/no-show', optionalAuth, bookingController.markBookingNoShow);
router.post('/:id/reminder', optionalAuth, bookingController.sendAppointmentReminder);

module.exports = router;
