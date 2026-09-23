package com.example.data.repository

import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.BookingEntity
import com.example.data.local.FavoriteEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.UserProfileEntity
import com.example.data.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

class BarberRepository(private val database: AppDatabase) {

    private val bookingDao = database.bookingDao()
    private val favoriteDao = database.favoriteDao()
    private val notificationDao = database.notificationDao()
    private val userProfileDao = database.userProfileDao()

    // In-memory catalog of shops & services (backed by real data structures)
    private val baseShops = listOf(
        BarberShop(
            id = "shop_1",
            name = "Royal Barber Club & Lounge",
            rating = 4.9,
            reviewCount = 1250,
            distanceKm = 1.2,
            startingPrice = 250,
            nextAvailableSlot = "5:30 PM",
            isOpen = true,
            address = "Sector 18, Wave Mall Complex, Noida",
            city = "Noida, Uttar Pradesh",
            imageDrawableRes = R.drawable.img_shop_royal,
            description = "Award-winning gentleman's grooming lounge featuring handcrafted leather styling chairs, bespoke hot towel shaves, precision fades, and complimentary craft espresso.",
            phone = "+91 98112 34567",
            services = listOf(
                BarberService("s1", "Signature Royal Haircut", "Haircut", 350, 30, "Consultation, wash, precision scissor/clipper cut & style"),
                BarberService("s2", "Royal Beard Sculpt & Trim", "Beard", 200, 20, "Hot towel treatment, straight razor lining & organic beard oil"),
                BarberService("s3", "Combo: Cut + Beard Craft", "Haircut", 500, 45, "Full signature haircut combined with beard sculpt & head wash"),
                BarberService("s4", "Revitalizing Charcoal Facial", "Facial", 650, 40, "Deep pore cleansing, steam exfoliation & detoxifying mask"),
                BarberService("s5", "Herbal Scalp Spa & Massage", "Hair Spa", 800, 50, "Invigorating scalp detox, deep conditioning massage & blowout"),
                BarberService("s6", "Executive Hair Color & Camo", "Hair Color", 900, 60, "Natural tone blending or full premium ammonia-free color")
            ),
            barbers = listOf(
                Barber("b1", "Rahul Sharma", 4.9, 6, "Master Barber / Scissor Specialist"),
                Barber("b2", "Amit Verma", 4.8, 4, "Skin Fade & Beard Architect"),
                Barber("b3", "Mohit Rana", 4.7, 5, "Classic Grooming & Hot Shaves")
            ),
            reviews = listOf(
                Review("r1", "Vikram Malhotra", 5.0, "Rahul gives the sharpest skin fade in Delhi-NCR! The ambience is luxurious.", "2 days ago", "Rahul Sharma"),
                Review("r2", "Siddharth Jain", 5.0, "Great attention to detail and zero waiting time when booked via BarberCraft.", "1 week ago", "Amit Verma"),
                Review("r3", "Rohan Gupta", 4.8, "The hot towel beard sculpt is unmatched. Highly recommended!", "2 weeks ago", "Rahul Sharma")
            )
        ),
        BarberShop(
            id = "shop_2",
            name = "Fade & Blade Studio",
            rating = 4.8,
            reviewCount = 840,
            distanceKm = 2.4,
            startingPrice = 200,
            nextAvailableSlot = "4:00 PM",
            isOpen = true,
            address = "Advant Navis Business Park, Sector 142, Noida",
            city = "Noida, Uttar Pradesh",
            imageDrawableRes = R.drawable.img_shop_fade,
            description = "Modern urban studio specializing in high-contrast taper fades, razor-sharp edge-ups, and contemporary textured styles.",
            phone = "+91 99554 11223",
            services = listOf(
                BarberService("s21", "Urban Skin Fade", "Haircut", 300, 30, "Razor taper/fade with precision edge-up & texturizing"),
                BarberService("s22", "Beard Styling & Lineup", "Beard", 150, 15, "Clipper trim, sharp foil shaver finish & balm"),
                BarberService("s23", "Combo: Fade & Sharp Beard", "Haircut", 420, 40, "Skin fade haircut paired with complete beard grooming"),
                BarberService("s24", "Gold Glow Express Facial", "Facial", 500, 30, "Instant radiance facial for special events"),
                BarberService("s25", "Kids Styling Cut", "Kids Haircut", 250, 25, "Patient, friendly styling cut for kids under 12")
            ),
            barbers = listOf(
                Barber("b21", "Devendra Rawat", 4.9, 7, "High-Contrast Fade Specialist"),
                Barber("b22", "Karan Kapoor", 4.7, 3, "Modern Textures & Lineups")
            ),
            reviews = listOf(
                Review("r21", "Manish Mehra", 5.0, "Fast, immaculate fade. Love the modern studio atmosphere!", "3 days ago", "Devendra Rawat"),
                Review("r22", "Ayush Taneja", 4.8, "Super clean shop, great music, and master-level barbers.", "2 weeks ago", "Karan Kapoor")
            )
        ),
        BarberShop(
            id = "shop_3",
            name = "Urban Gentleman Barbershop",
            rating = 4.7,
            reviewCount = 612,
            distanceKm = 3.8,
            startingPrice = 280,
            nextAvailableSlot = "6:15 PM",
            isOpen = true,
            address = "Sector 62, Candor TechSpace, Noida",
            city = "Noida, Uttar Pradesh",
            imageDrawableRes = R.drawable.img_hero_banner,
            description = "A refined grooming sanctuary for discerning men. Traditional craftsmanship combined with contemporary wellness treatments.",
            phone = "+91 97110 99887",
            services = listOf(
                BarberService("s31", "Classic Gentleman Cut", "Haircut", 380, 35, "Scissor work, clipper contour, and invigorating wash"),
                BarberService("s32", "Royal Shave & Face Massage", "Beard", 250, 25, "Traditional cut-throat razor shave with aromatic oils"),
                BarberService("s33", "Intensive Keratin Hair Spa", "Hair Spa", 950, 60, "Deep repair therapy for damaged hair with steam mask"),
                BarberService("s34", "Grey Hair Camouflage", "Hair Color", 850, 45, "Discreet, natural grey reduction in 15 minutes")
            ),
            barbers = listOf(
                Barber("b31", "Sameer Sheikh", 4.8, 5, "Scissor Mastery & Shaves"),
                Barber("b32", "Farhan Malik", 4.6, 4, "Hair Care & Spa Expert")
            ),
            reviews = listOf(
                Review("r31", "Deepak Verma", 4.9, "The straight razor shave was heavenly. Best in Sector 62.", "5 days ago", "Sameer Sheikh")
            )
        ),
        BarberShop(
            id = "shop_4",
            name = "The Barber's Den & Spa",
            rating = 4.6,
            reviewCount = 430,
            distanceKm = 5.1,
            startingPrice = 180,
            nextAvailableSlot = "Tomorrow, 10:00 AM",
            isOpen = false,
            address = "Atta Market, Sector 27, Noida",
            city = "Noida, Uttar Pradesh",
            imageDrawableRes = R.drawable.img_shop_royal,
            description = "Affordable excellence with seasoned barbers. Known for punctual service, quick walk-ins, and polite staff.",
            phone = "+91 98114 77665",
            services = listOf(
                BarberService("s41", "Quick Buzz & Fade", "Haircut", 180, 20, "Fast, clean clipper cut with neckline taper"),
                BarberService("s42", "Standard Beard Grooming", "Beard", 120, 15, "Trim and shape up"),
                BarberService("s43", "D-Tan Facial Clean-up", "Facial", 400, 30, "Anti-pollution tan removal scrub and pack")
            ),
            barbers = listOf(
                Barber("b41", "Imran Ali", 4.7, 8, "All-rounder Senior Stylist")
            ),
            reviews = listOf(
                Review("r41", "Tarun Saxena", 4.5, "Budget friendly and very skilled staff.", "1 month ago", "Imran Ali")
            )
        )
    )

    // Observable favorites from Room combined with shops
    fun getShopsFlow(): Flow<List<BarberShop>> {
        return favoriteDao.getAllFavorites().map { favorites ->
            val favIds = favorites.map { it.shopId }.toSet()
            baseShops.map { shop ->
                shop.copy(isFavorite = favIds.contains(shop.id))
            }
        }
    }

    suspend fun toggleFavorite(shopId: String) {
        val isFav = favoriteDao.isFavorite(shopId)
        if (isFav) {
            favoriteDao.removeFavorite(shopId)
        } else {
            favoriteDao.addFavorite(FavoriteEntity(shopId))
        }
    }

    // Bookings from Room
    fun getBookingsFlow(): Flow<List<Booking>> {
        return bookingDao.getAllBookings().map { entities ->
            entities.map { entity ->
                Booking(
                    id = entity.id,
                    shopId = entity.shopId,
                    shopName = entity.shopName,
                    shopAddress = entity.shopAddress,
                    serviceNames = entity.serviceNamesJoined.split(",").map { it.trim() },
                    barberName = entity.barberName,
                    dateStr = entity.dateStr,
                    timeSlot = entity.timeSlot,
                    price = entity.price,
                    tax = entity.tax,
                    total = entity.total,
                    status = runCatching { BookingStatus.valueOf(entity.status) }.getOrDefault(BookingStatus.UPCOMING),
                    paymentStatus = runCatching { PaymentStatus.valueOf(entity.paymentStatus) }.getOrDefault(PaymentStatus.PAID),
                    paymentMethod = entity.paymentMethod,
                    createdAt = entity.createdAt
                )
            }
        }
    }

    suspend fun saveBooking(booking: Booking) {
        bookingDao.insertBooking(
            BookingEntity(
                id = booking.id,
                shopId = booking.shopId,
                shopName = booking.shopName,
                shopAddress = booking.shopAddress,
                serviceNamesJoined = booking.serviceNames.joinToString(", "),
                barberName = booking.barberName,
                dateStr = booking.dateStr,
                timeSlot = booking.timeSlot,
                price = booking.price,
                tax = booking.tax,
                total = booking.total,
                status = booking.status.name,
                paymentStatus = booking.paymentStatus.name,
                paymentMethod = booking.paymentMethod,
                createdAt = booking.createdAt
            )
        )

        // Also add a confirmation notification
        notificationDao.insertNotification(
            NotificationEntity(
                id = "notif_" + System.currentTimeMillis(),
                title = "Booking Confirmed! 🎉",
                message = "Your appointment with ${booking.barberName} at ${booking.shopName} is confirmed for ${booking.timeSlot} on ${booking.dateStr}.",
                timeAgo = "Just now",
                type = NotificationType.CONFIRMED.name,
                isRead = false
            )
        )
    }

    suspend fun cancelBooking(bookingId: String) {
        bookingDao.updateBookingStatus(bookingId, BookingStatus.CANCELLED.name)
        notificationDao.insertNotification(
            NotificationEntity(
                id = "notif_" + System.currentTimeMillis(),
                title = "Booking Cancelled",
                message = "Your appointment ($bookingId) has been cancelled as per the cancellation policy.",
                timeAgo = "Just now",
                type = NotificationType.CANCELLED.name,
                isRead = false
            )
        )
    }

    suspend fun rescheduleBooking(bookingId: String, newDate: String, newTime: String) {
        bookingDao.rescheduleBooking(bookingId, newDate, newTime)
        notificationDao.insertNotification(
            NotificationEntity(
                id = "notif_" + System.currentTimeMillis(),
                title = "Booking Rescheduled ⏰",
                message = "Your appointment ($bookingId) is updated to $newTime on $newDate.",
                timeAgo = "Just now",
                type = NotificationType.RESCHEDULED.name,
                isRead = false
            )
        )
    }

    // Notifications
    fun getNotificationsFlow(): Flow<List<NotificationItem>> {
        return notificationDao.getAllNotifications().map { entities ->
            entities.map { entity ->
                NotificationItem(
                    id = entity.id,
                    title = entity.title,
                    message = entity.message,
                    timeAgo = entity.timeAgo,
                    type = runCatching { NotificationType.valueOf(entity.type) }.getOrDefault(NotificationType.CONFIRMED),
                    isRead = entity.isRead
                )
            }
        }
    }

    suspend fun markAllNotificationsAsRead() {
        notificationDao.markAllAsRead()
    }

    // User Profile
    fun getUserProfileFlow(): Flow<User> {
        return userProfileDao.getUserProfile().map { entity ->
            if (entity != null) {
                User(
                    id = entity.id,
                    name = entity.name,
                    email = entity.email,
                    phone = entity.phone
                )
            } else {
                User(
                    id = "usr_101",
                    name = "Akash Sharma",
                    email = "akash.sharma@example.com",
                    phone = "+91 98765 12340"
                )
            }
        }
    }

    suspend fun updateUserProfile(name: String, email: String, phone: String) {
        userProfileDao.saveUserProfile(
            UserProfileEntity(
                id = "usr_101",
                name = name,
                email = email,
                phone = phone
            )
        )
    }

    // Availability API simulation (/api/v1/barbers/{barberId}/availability)
    // Server is authoritatively source of truth
    suspend fun getAvailabilitySlots(shopId: String, barberId: String, date: String): Map<String, List<Pair<String, Boolean>>> {
        delay(300) // Realistic network round-trip simulation
        // Returns morning, afternoon, evening slots with availability flag (true = available, false = booked)
        val isToday = date.contains("23") || date.contains("Today")
        return mapOf(
            "Morning" to listOf(
                "09:30 AM" to !isToday,
                "10:00 AM" to true,
                "10:30 AM" to true,
                "11:00 AM" to false,
                "11:30 AM" to true
            ),
            "Afternoon" to listOf(
                "12:00 PM" to false,
                "12:30 PM" to true,
                "01:00 PM" to true,
                "02:00 PM" to true,
                "02:30 PM" to false,
                "03:00 PM" to true
            ),
            "Evening" to listOf(
                "04:30 PM" to true,
                "05:00 PM" to false,
                "05:30 PM" to true,
                "06:00 PM" to true,
                "06:30 PM" to false,
                "07:00 PM" to true,
                "07:30 PM" to true
            )
        )
    }

    // Temporary Slot Hold API simulation (/api/v1/bookings/hold)
    suspend fun holdSlot(shopId: String, slotTime: String, slotDate: String, simulateConflict: Boolean = false): Result<SlotReservation> {
        delay(400) // Simulated API call
        if (simulateConflict) {
            return Result.failure(Exception("HTTP 409 Conflict: This slot was just booked by another customer. Please select another slot."))
        }
        val reservation = SlotReservation(
            reservationId = "RES" + UUID.randomUUID().toString().take(6).uppercase(),
            shopId = shopId,
            slotTime = slotTime,
            slotDate = slotDate,
            expiresAtTimestamp = System.currentTimeMillis() + (10 * 60 * 1000) // 10 minutes hold
        )
        return Result.success(reservation)
    }

    // Payment Gateway simulation
    suspend fun processPayment(reservationId: String, amount: Int, paymentMethod: String): Result<String> {
        delay(800) // Network call to payment gateway
        val transactionId = "TXN_" + System.currentTimeMillis().toString().takeLast(8)
        return Result.success(transactionId)
    }

    // Seed database on first launch
    suspend fun seedInitialDataIfEmpty() {
        val existingBookings = bookingDao.getAllBookings()
        // Insert sample upcoming & completed bookings if empty
        // Insert sample notifications
        notificationDao.insertNotification(
            NotificationEntity(
                id = "notif_seed_1",
                title = "Welcome to BarberCraft! ✂️",
                message = "Discover top-rated barbers in Noida, view live availability and book seamlessly.",
                timeAgo = "1 hour ago",
                type = NotificationType.OFFER.name,
                isRead = false,
                timestamp = System.currentTimeMillis() - 3600000
            )
        )
        notificationDao.insertNotification(
            NotificationEntity(
                id = "notif_seed_2",
                title = "Flat 20% Off on Royal Haircut",
                message = "Use code GROOM20 at checkout for weekend bookings at Royal Barber Club.",
                timeAgo = "3 hours ago",
                type = NotificationType.OFFER.name,
                isRead = true,
                timestamp = System.currentTimeMillis() - 10800000
            )
        )

        // Seed 1 upcoming booking and 1 completed booking for rich demonstration
        bookingDao.insertBooking(
            BookingEntity(
                id = "BK10234",
                shopId = "shop_1",
                shopName = "Royal Barber Club & Lounge",
                shopAddress = "Sector 18, Wave Mall Complex, Noida",
                serviceNamesJoined = "Signature Royal Haircut, Royal Beard Sculpt & Trim",
                barberName = "Rahul Sharma",
                dateStr = "25 Sep 2026",
                timeSlot = "5:30 PM",
                price = 550,
                tax = 28,
                total = 578,
                status = BookingStatus.UPCOMING.name,
                paymentStatus = PaymentStatus.PAID.name,
                paymentMethod = "UPI (GPay)",
                createdAt = System.currentTimeMillis() - 7200000
            )
        )
        bookingDao.insertBooking(
            BookingEntity(
                id = "BK09821",
                shopId = "shop_2",
                shopName = "Fade & Blade Studio",
                shopAddress = "Sector 142, Advant Navis, Noida",
                serviceNamesJoined = "Urban Skin Fade",
                barberName = "Devendra Rawat",
                dateStr = "18 Sep 2026",
                timeSlot = "4:00 PM",
                price = 300,
                tax = 15,
                total = 315,
                status = BookingStatus.COMPLETED.name,
                paymentStatus = PaymentStatus.PAID.name,
                paymentMethod = "Credit Card",
                createdAt = System.currentTimeMillis() - 500000000
            )
        )

        // User profile initial seed
        userProfileDao.saveUserProfile(
            UserProfileEntity(
                id = "usr_101",
                name = "Akash Sharma",
                email = "akash.sharma@example.com",
                phone = "+91 98765 12340"
            )
        )
    }
}
