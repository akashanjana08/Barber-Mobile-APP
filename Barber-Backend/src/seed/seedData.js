const User = require('../models/User');
const BarberShop = require('../models/BarberShop');
const Booking = require('../models/Booking');
const BarberQueue = require('../models/BarberQueue');
const SlotOffer = require('../models/SlotOffer');
const Notification = require('../models/Notification');
const Review = require('../models/Review');

const seedInitialData = async () => {
  try {
    console.log('[Seeder] Starting initial data seeding for BarberCraft...');

    // 1. Users
    const existingUser = await User.findOne({ email: 'akash.sharma@example.com' });
    let akashUser = existingUser;

    if (!existingUser) {
      akashUser = await User.create({
        name: 'Akash Sharma',
        email: 'akash.sharma@example.com',
        phone: '+91 98765 12340',
        role: 'customer',
        location: { lat: 28.6500, lng: 77.4500, address: 'Sector 18, Wave Mall Complex, Noida' },
        locationPermissionGranted: true,
        favoriteShops: ['shop_1'],
        lastActiveAt: new Date(),
      });

      await User.insertMany([
        {
          name: 'Vikram Malhotra',
          email: 'vikram.m@example.com',
          phone: '+91 98123 45678',
          role: 'customer',
          location: { lat: 28.6510, lng: 77.4520, address: 'Sector 18, Noida' },
          locationPermissionGranted: true,
          lastActiveAt: new Date(Date.now() - 3600000 * 2),
        },
        {
          name: 'Siddharth Jain',
          email: 'siddharth.j@example.com',
          phone: '+91 98234 56789',
          role: 'customer',
          location: { lat: 28.6480, lng: 77.4540, address: 'Sector 16, Noida' },
          locationPermissionGranted: true,
          lastActiveAt: new Date(Date.now() - 3600000 * 5),
        },
        {
          name: 'Rohan Gupta',
          email: 'rohan.g@example.com',
          phone: '+91 98345 67890',
          role: 'customer',
          location: { lat: 28.6420, lng: 77.4600, address: 'Sector 27, Noida' },
          locationPermissionGranted: true,
          lastActiveAt: new Date(Date.now() - 3600000 * 12),
        },
        {
          name: 'Nitin Kumar',
          email: 'nitin.k@example.com',
          phone: '+91 98456 78901',
          role: 'customer',
          location: { lat: 28.6600, lng: 77.4400, address: 'Sector 29, Noida' },
          locationPermissionGranted: true,
          lastActiveAt: new Date(Date.now() - 3600000 * 8),
        },
      ]);
      console.log('[Seeder] Users seeded.');
    }

    // 2. Shops
    const existingShops = await BarberShop.countDocuments();
    if (existingShops === 0) {
      await BarberShop.insertMany([
        {
          shopId: 'shop_1',
          name: 'Royal Barber Club & Lounge',
          rating: 4.8,
          reviewCount: 1250,
          distanceKm: 1.2,
          startingPrice: 250,
          nextAvailableSlot: '5:30 PM',
          isOpen: true,
          address: 'Sector 18, Wave Mall Complex, Noida',
          city: 'Noida, Uttar Pradesh',
          location: { type: 'Point', coordinates: [77.4500, 28.6500] },
          description:
            "Award-winning gentleman's grooming lounge featuring handcrafted leather styling chairs, bespoke hot towel shaves, precision fades, and complimentary craft espresso.",
          phone: '+91 98112 34567',
          services: [
            { id: 's1', name: 'Signature Royal Haircut', category: 'Haircut', price: 300, durationMin: 30, description: 'Consultation, wash, precision scissor/clipper cut & style' },
            { id: 's2', name: 'Royal Beard Sculpt & Trim', category: 'Beard', price: 200, durationMin: 20, description: 'Hot towel treatment, straight razor lining & organic beard oil' },
            { id: 's3', name: 'Combo: Haircut + Beard', category: 'Combo', price: 500, durationMin: 45, description: 'Full signature haircut combined with beard sculpt & head wash' },
            { id: 's4', name: 'Revitalizing Charcoal Facial', category: 'Facial', price: 650, durationMin: 40, description: 'Deep pore cleansing, steam exfoliation & detoxifying mask' },
            { id: 's5', name: 'Herbal Scalp Spa & Massage', category: 'Hair Spa', price: 800, durationMin: 50, description: 'Invigorating scalp detox, deep conditioning massage & blowout' },
            { id: 's6', name: 'Executive Hair Color & Camo', category: 'Hair Color', price: 900, durationMin: 60, description: 'Natural tone blending or full premium ammonia-free color' },
          ],
          barbers: [
            { id: 'b1', name: 'Rahul Sharma', rating: 4.9, experienceYears: 6, specialty: 'Master Barber / Scissor Specialist', isAvailable: true },
            { id: 'b2', name: 'Amit Verma', rating: 4.8, experienceYears: 4, specialty: 'Skin Fade & Beard Architect', isAvailable: true },
            { id: 'b3', name: 'Mohit Rana', rating: 4.7, experienceYears: 5, specialty: 'Classic Grooming & Hot Shaves', isAvailable: true },
          ],
          reviews: [
            { id: 'r1', userName: 'Vikram Malhotra', rating: 5.0, comment: 'Rahul gives the sharpest skin fade in Delhi-NCR! The ambience is luxurious.', date: '2 days ago', barberName: 'Rahul Sharma' },
            { id: 'r2', userName: 'Siddharth Jain', rating: 5.0, comment: 'Great attention to detail and zero waiting time when booked via BarberCraft.', date: '1 week ago', barberName: 'Amit Verma' },
            { id: 'r3', userName: 'Rohan Gupta', rating: 4.8, comment: 'The hot towel beard sculpt is unmatched. Highly recommended!', date: '2 weeks ago', barberName: 'Rahul Sharma' },
          ],
        },
        {
          shopId: 'shop_2',
          name: 'Fade & Blade Studio',
          rating: 4.8,
          reviewCount: 840,
          distanceKm: 2.4,
          startingPrice: 200,
          nextAvailableSlot: '4:00 PM',
          isOpen: true,
          address: 'Advant Navis Business Park, Sector 142, Noida',
          city: 'Noida, Uttar Pradesh',
          location: { type: 'Point', coordinates: [77.4600, 28.6300] },
          description: 'Modern urban studio specializing in high-contrast taper fades, razor-sharp edge-ups, and contemporary textured styles.',
          phone: '+91 99554 11223',
          services: [
            { id: 's21', name: 'Urban Skin Fade', category: 'Haircut', price: 300, durationMin: 30, description: 'Razor taper/fade with precision edge-up & texturizing' },
            { id: 's22', name: 'Beard Styling & Lineup', category: 'Beard', price: 150, durationMin: 15, description: 'Clipper trim, sharp foil shaver finish & balm' },
            { id: 's23', name: 'Combo: Fade & Sharp Beard', category: 'Combo', price: 420, durationMin: 40, description: 'Skin fade haircut paired with complete beard grooming' },
            { id: 's24', name: 'Gold Glow Express Facial', category: 'Facial', price: 500, durationMin: 30, description: 'Instant radiance facial for special events' },
            { id: 's25', name: 'Kids Styling Cut', category: 'Kids Haircut', price: 250, durationMin: 25, description: 'Patient, friendly styling cut for kids under 12' },
          ],
          barbers: [
            { id: 'b21', name: 'Devendra Rawat', rating: 4.9, experienceYears: 7, specialty: 'High-Contrast Fade Specialist', isAvailable: true },
            { id: 'b22', name: 'Karan Kapoor', rating: 4.7, experienceYears: 3, specialty: 'Modern Textures & Lineups', isAvailable: true },
          ],
          reviews: [
            { id: 'r21', userName: 'Manish Mehra', rating: 5.0, comment: 'Fast, immaculate fade. Love the modern studio atmosphere!', date: '3 days ago', barberName: 'Devendra Rawat' },
            { id: 'r22', userName: 'Ayush Taneja', rating: 4.8, comment: 'Super clean shop, great music, and master-level barbers.', date: '2 weeks ago', barberName: 'Karan Kapoor' },
          ],
        },
        {
          shopId: 'shop_3',
          name: 'Urban Gentleman Barbershop',
          rating: 4.7,
          reviewCount: 612,
          distanceKm: 3.8,
          startingPrice: 280,
          nextAvailableSlot: '6:15 PM',
          isOpen: true,
          address: 'Sector 62, Candor TechSpace, Noida',
          city: 'Noida, Uttar Pradesh',
          location: { type: 'Point', coordinates: [77.4700, 28.6200] },
          description: 'A refined grooming sanctuary for discerning men. Traditional craftsmanship combined with contemporary wellness treatments.',
          phone: '+91 97110 99887',
          services: [
            { id: 's31', name: 'Classic Gentleman Cut', category: 'Haircut', price: 380, durationMin: 35, description: 'Scissor work, clipper contour, and invigorating wash' },
            { id: 's32', name: 'Royal Shave & Face Massage', category: 'Beard', price: 250, durationMin: 25, description: 'Traditional cut-throat razor shave with aromatic oils' },
            { id: 's33', name: 'Intensive Keratin Hair Spa', category: 'Hair Spa', price: 950, durationMin: 60, description: 'Deep repair therapy for damaged hair with steam mask' },
            { id: 's34', name: 'Grey Hair Camouflage', category: 'Hair Color', price: 850, durationMin: 45, description: 'Discreet, natural grey reduction in 15 minutes' },
          ],
          barbers: [
            { id: 'b31', name: 'Sameer Sheikh', rating: 4.8, experienceYears: 5, specialty: 'Scissor Mastery & Shaves', isAvailable: true },
            { id: 'b32', name: 'Farhan Malik', rating: 4.6, experienceYears: 4, specialty: 'Hair Care & Spa Expert', isAvailable: true },
          ],
          reviews: [
            { id: 'r31', userName: 'Deepak Verma', rating: 4.9, comment: 'The straight razor shave was heavenly. Best in Sector 62.', date: '5 days ago', barberName: 'Sameer Sheikh' },
          ],
        },
        {
          shopId: 'shop_4',
          name: "The Barber's Den & Spa",
          rating: 4.6,
          reviewCount: 430,
          distanceKm: 5.1,
          startingPrice: 180,
          nextAvailableSlot: 'Tomorrow, 10:00 AM',
          isOpen: false,
          address: 'Atta Market, Sector 27, Noida',
          city: 'Noida, Uttar Pradesh',
          location: { type: 'Point', coordinates: [77.4350, 28.6600] },
          description: 'Affordable excellence with seasoned barbers. Known for punctual service, quick walk-ins, and polite staff.',
          phone: '+91 98114 77665',
          services: [
            { id: 's41', name: 'Quick Buzz & Fade', category: 'Haircut', price: 180, durationMin: 20, description: 'Fast, clean clipper cut with neckline taper' },
            { id: 's42', name: 'Standard Beard Grooming', category: 'Beard', price: 120, durationMin: 15, description: 'Trim and shape up' },
            { id: 's43', name: 'D-Tan Facial Clean-up', category: 'Facial', price: 400, durationMin: 30, description: 'Anti-pollution tan removal scrub and pack' },
          ],
          barbers: [
            { id: 'b41', name: 'Imran Ali', rating: 4.7, experienceYears: 8, specialty: 'All-rounder Senior Stylist', isAvailable: true },
          ],
          reviews: [
            { id: 'r41', userName: 'Tarun Saxena', rating: 4.5, comment: 'Budget friendly and very skilled staff.', date: '1 month ago', barberName: 'Imran Ali' },
          ],
        },
      ]);
      console.log('[Seeder] Barber Shops seeded.');
    }

    // 3. Bookings
    const existingBookings = await Booking.countDocuments();
    if (existingBookings === 0) {
      await Booking.insertMany([
        {
          bookingNumber: 'BK10234',
          userId: akashUser?._id,
          customerName: 'Akash Sharma',
          customerPhone: '+91 98765 12340',
          shopId: 'shop_1',
          shopName: 'Royal Barber Club & Lounge',
          shopAddress: 'Sector 18, Wave Mall Complex, Noida',
          serviceNames: ['Signature Royal Haircut', 'Royal Beard Sculpt & Trim'],
          barberName: 'Rahul Sharma',
          dateStr: '25 Sep 2026',
          timeSlot: '5:30 PM',
          scheduledTimeMinutes: 1050,
          durationMinutes: 45,
          price: 500,
          tax: 25,
          total: 525,
          status: 'UPCOMING',
          paymentStatus: 'PAID',
          paymentMethod: 'UPI (Google Pay)',
          transactionId: 'TXN_9823411',
          reminderSentAt: null,
          isLate: false,
        },
        {
          bookingNumber: 'BK09821',
          userId: akashUser?._id,
          customerName: 'Akash Sharma',
          customerPhone: '+91 98765 12340',
          shopId: 'shop_2',
          shopName: 'Fade & Blade Studio',
          shopAddress: 'Sector 142, Advant Navis, Noida',
          serviceNames: ['Urban Skin Fade'],
          barberName: 'Devendra Rawat',
          dateStr: '18 Sep 2026',
          timeSlot: '4:00 PM',
          scheduledTimeMinutes: 960,
          durationMinutes: 30,
          price: 300,
          tax: 15,
          total: 315,
          status: 'COMPLETED',
          paymentStatus: 'PAID',
          paymentMethod: 'Credit Card',
          transactionId: 'TXN_8711094',
          reminderSentAt: new Date(Date.now() - 500000000),
          isLate: false,
        },
      ]);
      console.log('[Seeder] Bookings seeded.');
    }

    // 4. Real-time Barber Queue Benchmark Items
    const existingQueue = await BarberQueue.countDocuments();
    if (existingQueue === 0) {
      await BarberQueue.insertMany([
        // Customer A: 5:00 PM, IN_PROGRESS (15 min remaining)
        {
          queueId: 'Q_RAHUL_1',
          barberId: 'b1',
          barberName: 'Rahul Sharma',
          shopId: 'shop_1',
          customerName: 'Customer A (Vikram M.)',
          isCurrentUser: false,
          bookingId: 'BK_EXT_01',
          serviceNames: ['Signature Royal Haircut'],
          durationMinutes: 30,
          scheduledTime: '5:00 PM',
          scheduledTimeMinutes: 1020,
          status: 'IN_PROGRESS',
          remainingMinutes: 15,
          delayMinutes: 0,
        },
        // Customer B: 5:15 PM, CHECKED_IN (15 min duration)
        {
          queueId: 'Q_RAHUL_2',
          barberId: 'b1',
          barberName: 'Rahul Sharma',
          shopId: 'shop_1',
          customerName: 'Customer B (Siddharth J.)',
          isCurrentUser: false,
          bookingId: 'BK_EXT_02',
          serviceNames: ['Royal Beard Sculpt & Trim'],
          durationMinutes: 15,
          scheduledTime: '5:15 PM',
          scheduledTimeMinutes: 1035,
          status: 'CHECKED_IN',
          remainingMinutes: 15,
          delayMinutes: 0,
        },
        // Current User: 5:30 PM, CONFIRMED (matches BK10234)
        {
          queueId: 'Q_RAHUL_CURRENT',
          barberId: 'b1',
          barberName: 'Rahul Sharma',
          shopId: 'shop_1',
          customerName: 'You (Akash Sharma)',
          isCurrentUser: true,
          userId: akashUser?._id,
          bookingId: 'BK10234',
          serviceNames: ['Signature Royal Haircut', 'Royal Beard Sculpt & Trim'],
          durationMinutes: 45,
          scheduledTime: '5:30 PM',
          scheduledTimeMinutes: 1050,
          status: 'CONFIRMED',
          remainingMinutes: 45,
          delayMinutes: 0,
        },
        // Customer C: 5:45 PM, CONFIRMED (after 5:30 PM, not counted before you)
        {
          queueId: 'Q_RAHUL_3',
          barberId: 'b1',
          barberName: 'Rahul Sharma',
          shopId: 'shop_1',
          customerName: 'Customer C (Rohan G.)',
          isCurrentUser: false,
          bookingId: 'BK_EXT_03',
          serviceNames: ['Signature Royal Haircut'],
          durationMinutes: 30,
          scheduledTime: '5:45 PM',
          scheduledTimeMinutes: 1065,
          status: 'CONFIRMED',
          remainingMinutes: 30,
          delayMinutes: 0,
        },
        // Amit Verma's Queue (1 customer waiting)
        {
          queueId: 'Q_AMIT_1',
          barberId: 'b2',
          barberName: 'Amit Verma',
          shopId: 'shop_1',
          customerName: 'Customer D (Nitin K.)',
          isCurrentUser: false,
          bookingId: 'BK_EXT_04',
          serviceNames: ['Skin Fade & Edge-up'],
          durationMinutes: 15,
          scheduledTime: '5:00 PM',
          scheduledTimeMinutes: 1020,
          status: 'CONFIRMED',
          remainingMinutes: 15,
          delayMinutes: 0,
        },
      ]);
      console.log('[Seeder] Barber Queue benchmark items seeded.');
    }

    // 5. Slot Recovery Offer (Flash Offer)
    const existingOffers = await SlotOffer.countDocuments();
    if (existingOffers === 0) {
      await SlotOffer.create({
        offerCode: 'OFFER_ROYAL_530',
        slotId: 'SLOT_shop_1_530PM',
        shopId: 'shop_1',
        shopName: 'Royal Barber Club & Lounge',
        shopAddress: 'Sector 18, Wave Mall Complex, Noida',
        shopRating: 4.8,
        shopLat: 28.6500,
        shopLng: 77.4500,
        serviceName: 'Signature Royal Haircut',
        barberName: 'Rahul Sharma',
        dateStr: 'Today, 25 Sep',
        timeSlot: '5:30 PM',
        originalPrice: 300,
        discountPercent: 20,
        offerPrice: 240,
        expiresAt: new Date(Date.now() + 35 * 60 * 1000), // 35 minutes left
        bufferMinutes: 15,
        status: 'SENT',
        sourceType: 'CUSTOMER_CANCELLATION',
        distanceKm: 1.2,
      });
      console.log('[Seeder] Active Slot Offer seeded.');
    }

    // 6. Notifications
    const existingNotifications = await Notification.countDocuments();
    if (existingNotifications === 0) {
      await Notification.insertMany([
        {
          notificationId: 'notif_rem_seed',
          userId: akashUser?._id,
          title: '⏰ Appointment Reminder',
          message:
            'Your appointment at Royal Barber Club & Lounge starts in 15 minutes.\n\nService: Haircut + Beard\nBarber: Rahul Sharma\nTime: 5:30 PM\n\nPlease reach the barber shop before your scheduled time.',
          type: 'REMINDER',
          isRead: false,
          relatedBookingId: 'BK10234',
        },
        {
          notificationId: 'notif_offer_seed',
          userId: akashUser?._id,
          title: '🔥 Last-Minute Barber Offer',
          message:
            'A slot just became available near you!\n\nRoyal Barber Club & Lounge ⭐ 4.8\nHaircut\n₹300 → ₹240 (20% OFF)\nToday 5:30 PM\n📍 1.2 km away\n\nThis offer is available for a limited time.',
          type: 'OFFER',
          isRead: false,
          relatedOfferId: 'OFFER_ROYAL_530',
        },
      ]);
      console.log('[Seeder] Notifications seeded.');
    }

    console.log('[Seeder] All database collections initialized successfully!');
  } catch (error) {
    console.error('[Seeder Error] Failed to seed database:', error);
  }
};

module.exports = {
  seedInitialData,
};
