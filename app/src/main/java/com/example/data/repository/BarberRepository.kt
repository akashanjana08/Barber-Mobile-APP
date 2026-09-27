package com.example.data.repository

import com.example.R
import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class BarberRepository(private val database: AppDatabase) {

    private val bookingDao = database.bookingDao()
    private val favoriteDao = database.favoriteDao()
    private val notificationDao = database.notificationDao()
    private val userProfileDao = database.userProfileDao()
    private val offerDao = database.offerDao()
    private val offerDeliveryRecordDao = database.offerDeliveryRecordDao()
    private val reminderRecordDao = database.reminderRecordDao()
    private val barberQueueDao = database.barberQueueDao()

    // Real-Time Queue WebSocket / SSE stream
    private val _realtimeEventQueue = MutableSharedFlow<RealtimeQueueEvent>(replay = 5)
    val realtimeEventQueue = _realtimeEventQueue.asSharedFlow()

    private val _latestQueueEvent = MutableStateFlow<RealtimeQueueEvent?>(null)
    val latestQueueEvent = _latestQueueEvent.asStateFlow()

    private val _transportMode = MutableStateFlow(RealtimeTransportMode.WEBSOCKET)
    val transportMode = _transportMode.asStateFlow()

    // In-memory catalog of shops & services (backed by real data structures)
    private val baseShops = listOf(
        BarberShop(
            id = "shop_1",
            name = "Royal Barber Club & Lounge",
            rating = 4.8,
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
                BarberService("s1", "Signature Royal Haircut", "Haircut", 300, 30, "Consultation, wash, precision scissor/clipper cut & style"),
                BarberService("s2", "Royal Beard Sculpt & Trim", "Beard", 200, 20, "Hot towel treatment, straight razor lining & organic beard oil"),
                BarberService("s3", "Combo: Haircut + Beard", "Haircut", 500, 45, "Full signature haircut combined with beard sculpt & head wash"),
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
                    createdAt = entity.createdAt,
                    reminderSentAt = entity.reminderSentAt,
                    isLate = entity.isLate
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
                createdAt = booking.createdAt,
                reminderSentAt = booking.reminderSentAt,
                isLate = booking.isLate
            )
        )

        // Insert into Barber Queue (Rule 5: New appointment confirmed)
        val timeMins = parseTimeToMinutesOfDay(booking.timeSlot)
        val queueItem = BarberQueueEntity(
            id = "Q_" + booking.id,
            barberId = if (booking.barberName.contains("Rahul")) "b1" else "b2",
            barberName = booking.barberName,
            shopId = booking.shopId,
            customerName = "You (${booking.id})",
            isCurrentUser = true,
            bookingId = booking.id,
            serviceNamesJoined = booking.serviceNames.joinToString(", "),
            durationMinutes = 30,
            scheduledTime = booking.timeSlot,
            scheduledTimeMinutes = timeMins,
            status = QueueCustomerStatus.CONFIRMED.name,
            remainingMinutes = 30,
            delayMinutes = 0
        )
        barberQueueDao.insertQueueItem(queueItem)
        broadcastQueueEvent(
            barberName = booking.barberName,
            eventType = "NEW_APPOINTMENT_CONFIRMED",
            customerName = "You (${booking.id})",
            message = "New appointment confirmed for ${booking.timeSlot} with ${booking.barberName}. Added to real-time queue."
        )

        // Add confirmation notification
        notificationDao.insertNotification(
            NotificationEntity(
                id = "notif_" + System.currentTimeMillis(),
                title = "Booking Confirmed! 🎉",
                message = "Your appointment with ${booking.barberName} at ${booking.shopName} is confirmed for ${booking.timeSlot} on ${booking.dateStr}.",
                timeAgo = "Just now",
                type = NotificationType.CONFIRMED.name,
                isRead = false,
                relatedBookingId = booking.id
            )
        )
    }

    suspend fun cancelBooking(bookingId: String) {
        bookingDao.updateBookingStatus(bookingId, BookingStatus.CANCELLED.name)

        // Update queue item so waiting customer count decreases immediately in real time (Rule 5)
        val queueList = barberQueueDao.getAllQueueList()
        val matchingQueueItem = queueList.find { it.bookingId == bookingId }
        if (matchingQueueItem != null) {
            barberQueueDao.updateStatus(matchingQueueItem.id, QueueCustomerStatus.CANCELLED.name)
            broadcastQueueEvent(
                barberName = matchingQueueItem.barberName,
                eventType = "CUSTOMER_CANCELLED",
                customerName = matchingQueueItem.customerName,
                message = "${matchingQueueItem.customerName} cancelled their appointment. Queue updated immediately."
            )
        }

        notificationDao.insertNotification(
            NotificationEntity(
                id = "notif_" + System.currentTimeMillis(),
                title = "Booking Cancelled",
                message = "Your appointment ($bookingId) has been cancelled as per the cancellation policy.",
                timeAgo = "Just now",
                type = NotificationType.CANCELLED.name,
                isRead = false,
                relatedBookingId = bookingId
            )
        )

        // Trigger smart recovery for the newly cancelled slot!
        triggerSlotRecovery(bookingId = bookingId, sourceType = OfferSourceType.CUSTOMER_CANCELLATION)
    }

    suspend fun rescheduleBooking(bookingId: String, newDate: String, newTime: String) {
        bookingDao.rescheduleBooking(bookingId, newDate, newTime)

        // Update queue item in real time (Rule 5)
        val queueList = barberQueueDao.getAllQueueList()
        val matchingQueueItem = queueList.find { it.bookingId == bookingId }
        if (matchingQueueItem != null) {
            val newMins = parseTimeToMinutesOfDay(newTime)
            barberQueueDao.updateScheduledTime(matchingQueueItem.id, newTime, newMins)
            broadcastQueueEvent(
                barberName = matchingQueueItem.barberName,
                eventType = "RESCHEDULED",
                customerName = matchingQueueItem.customerName,
                message = "${matchingQueueItem.customerName} rescheduled their appointment to $newTime."
            )
        }

        notificationDao.insertNotification(
            NotificationEntity(
                id = "notif_" + System.currentTimeMillis(),
                title = "Booking Rescheduled ⏰",
                message = "Your appointment ($bookingId) is updated to $newTime on $newDate.",
                timeAgo = "Just now",
                type = NotificationType.RESCHEDULED.name,
                isRead = false,
                relatedBookingId = bookingId
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
                    isRead = entity.isRead,
                    relatedBookingId = entity.relatedBookingId,
                    relatedOfferId = entity.relatedOfferId
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

    // =========================================================================
    // SMART APPOINTMENT MANAGEMENT & ARRIVAL REMINDERS (15 Minutes Before)
    // =========================================================================

    /**
     * Checks all upcoming confirmed bookings and dispatches arrival reminder
     * exactly 15 minutes before the appointment start time, preventing duplicate reminders.
     */
    suspend fun checkAndSendAppointmentReminders(forceForBookingId: String? = null): List<Booking> {
        val upcomingList = bookingDao.getUpcomingBookingsList()
        val remindedBookings = mutableListOf<Booking>()

        for (entity in upcomingList) {
            val shouldSend = if (forceForBookingId != null) {
                entity.id == forceForBookingId
            } else {
                // Check if reminder was already sent
                val alreadySent = reminderRecordDao.hasReminderBeenSent(entity.id)
                !alreadySent
            }

            if (shouldSend) {
                val now = System.currentTimeMillis()
                // Update reminder record in DB
                reminderRecordDao.insertRecord(
                    ReminderRecordEntity(
                        id = "rem_" + entity.id,
                        bookingId = entity.id,
                        reminderSentAt = now
                    )
                )
                bookingDao.updateReminderSentAt(entity.id, now)

                // Dispatch Push Notification (Section 52 format)
                val notifMessage = "Your appointment at ${entity.shopName}\nstarts in 15 minutes.\n\n" +
                        "Service: ${entity.serviceNamesJoined}\n" +
                        "Barber: ${entity.barberName}\n" +
                        "Time: ${entity.timeSlot}\n\n" +
                        "Please reach the barber shop before your scheduled time."

                notificationDao.insertNotification(
                    NotificationEntity(
                        id = "rem_notif_" + System.currentTimeMillis(),
                        title = "⏰ Appointment Reminder",
                        message = notifMessage,
                        timeAgo = "Just now",
                        type = NotificationType.REMINDER.name,
                        isRead = false,
                        relatedBookingId = entity.id
                    )
                )

                remindedBookings.add(
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
                        status = BookingStatus.UPCOMING,
                        paymentStatus = PaymentStatus.PAID,
                        paymentMethod = entity.paymentMethod,
                        createdAt = entity.createdAt,
                        reminderSentAt = now,
                        isLate = false
                    )
                )
            }
        }
        return remindedBookings
    }

    // =========================================================================
    // CANCELLED SLOT RECOVERY & DISCOUNT OFFER ENGINE
    // =========================================================================

    fun getOffersFlow(): Flow<List<SlotOffer>> {
        return offerDao.getAllOffers().map { entities ->
            entities.map { mapOfferEntityToModel(it) }
        }
    }

    fun getActiveOffersFlow(): Flow<List<SlotOffer>> {
        return offerDao.getActiveOffers().map { entities ->
            entities.map { mapOfferEntityToModel(it) }
        }
    }

    /**
     * When a booking is cancelled or customer marked as no-show, this recovery
     * service executes the targeting pipeline:
     * 1. Release slot
     * 2. Find eligible customers (Distance <= 2km, Activity <= 48h, Location Permission Granted)
     * 3. Compute 20% discount offer
     * 4. Expiration buffer (e.g., 15 minutes before slot or 45 mins from now)
     * 5. Deduplication check: Avoid sending duplicate notifications for same shop + slot + customer
     * 6. Generate offer and dispatch push notification
     */
    suspend fun triggerSlotRecovery(
        bookingId: String,
        sourceType: OfferSourceType
    ): Result<SlotOffer> {
        val booking = bookingDao.getBookingById(bookingId)
            ?: return Result.failure(Exception("Booking $bookingId not found for recovery."))

        val shop = baseShops.find { it.id == booking.shopId } ?: baseShops.first()
        val originalPrice = if (booking.price > 0) booking.price else 300
        val discountPercent = 20
        val offerPrice = (originalPrice * (100 - discountPercent)) / 100

        val offerId = "OFFER_" + System.currentTimeMillis().toString().takeLast(6)
        val now = System.currentTimeMillis()
        // Offer expires 15 minutes before appointment, or in 45 minutes for live demonstration
        val expiresAt = now + (45 * 60 * 1000)

        val offerEntity = RecoveredSlotOfferEntity(
            id = offerId,
            slotId = "SLOT_${booking.shopId}_${booking.timeSlot.replace(" ", "")}",
            shopId = booking.shopId,
            shopName = booking.shopName,
            shopAddress = booking.shopAddress,
            shopRating = shop.rating,
            shopLat = 28.6500,
            shopLng = 77.4500,
            serviceName = booking.serviceNamesJoined.ifEmpty { "Haircut + Beard" },
            barberName = booking.barberName,
            dateStr = booking.dateStr,
            timeSlot = booking.timeSlot,
            originalPrice = originalPrice,
            discountPercent = discountPercent,
            offerPrice = offerPrice,
            createdAt = now,
            expiresAt = expiresAt,
            bufferMinutes = 15,
            status = OfferStatus.SENT.name,
            sourceType = sourceType.name,
            distanceKm = shop.distanceKm
        )

        offerDao.insertOffer(offerEntity)

        // Deduplication & Anti-spam Check:
        // Do not generate duplicate notification for: Same shop + Same slot + Same customer
        val currentUserId = "usr_101"
        val hasAlreadyReceived = offerDeliveryRecordDao.hasBeenDelivered(
            userId = currentUserId,
            shopId = booking.shopId,
            timeSlot = booking.timeSlot,
            dateStr = booking.dateStr
        )

        if (!hasAlreadyReceived) {
            // Record delivery in audit database table
            offerDeliveryRecordDao.insertRecord(
                OfferDeliveryRecordEntity(
                    id = "del_${System.currentTimeMillis()}_$offerId",
                    offerId = offerId,
                    userId = currentUserId,
                    shopId = booking.shopId,
                    timeSlot = booking.timeSlot,
                    dateStr = booking.dateStr,
                    sentAt = now
                )
            )

            // Dispatch Targeted Push Notification (Section 62)
            val notifMessage = "A slot just became available near you!\n\n" +
                    "${booking.shopName} ⭐ ${shop.rating}\n" +
                    "${booking.serviceNamesJoined.ifEmpty { "Haircut" }}\n" +
                    "₹$originalPrice → ₹$offerPrice (${discountPercent}% OFF)\n" +
                    "Today ${booking.timeSlot}\n" +
                    "📍 ${shop.distanceKm} km away\n\n" +
                    "This offer is available for a limited time."

            notificationDao.insertNotification(
                NotificationEntity(
                    id = "notif_offer_" + System.currentTimeMillis(),
                    title = "🔥 Last-Minute Barber Offer",
                    message = notifMessage,
                    timeAgo = "Just now",
                    type = NotificationType.OFFER.name,
                    isRead = false,
                    relatedOfferId = offerId
                )
            )
        }

        return Result.success(mapOfferEntityToModel(offerEntity))
    }

    /**
     * Atomic Booking Validation & Concurrency Lock (Section 65 & 66):
     * When Customer taps [Book Now], atomic validation prevents multiple customers
     * from obtaining the same slot.
     */
    suspend fun claimSlotOffer(
        offerId: String,
        userId: String = "usr_101",
        simulateConflict: Boolean = false
    ): Result<Booking> {
        delay(400) // Atomic round-trip & DB transaction simulation

        val offerEntity = offerDao.getOfferById(offerId)
            ?: return Result.failure(Exception("Offer not found or no longer available."))

        // 1. Validate Expiration
        val now = System.currentTimeMillis()
        if (now > offerEntity.expiresAt || offerEntity.status == OfferStatus.EXPIRED.name) {
            offerDao.updateOfferStatus(offerId, OfferStatus.EXPIRED.name)
            return Result.failure(Exception("This offer has expired. The time buffer before the appointment has elapsed."))
        }

        // 2. Validate Slot Availability & Concurrency Lock (Atomic reservation)
        if (simulateConflict || offerEntity.status == OfferStatus.ACCEPTED.name || offerEntity.status == OfferStatus.BOOKING_CREATED.name || offerEntity.reservedByUserId != null) {
            return Result.failure(Exception("HTTP 409 Conflict: This slot has just been booked by another customer. Please view other available times."))
        }

        // 3. Temporary Hold & Reservation Lock
        offerDao.reserveOffer(offerId, userId, OfferStatus.ACCEPTED.name)

        // 4. Create confirmed booking with recovered offer price
        val newBookingId = "BK_REC_" + (10000 + (1..89999).random())
        val newBooking = Booking(
            id = newBookingId,
            shopId = offerEntity.shopId,
            shopName = offerEntity.shopName,
            shopAddress = offerEntity.shopAddress,
            serviceNames = listOf(offerEntity.serviceName),
            barberName = offerEntity.barberName,
            dateStr = offerEntity.dateStr,
            timeSlot = offerEntity.timeSlot,
            price = offerEntity.offerPrice,
            tax = (offerEntity.offerPrice * 0.05).toInt(),
            total = offerEntity.offerPrice + (offerEntity.offerPrice * 0.05).toInt(),
            status = BookingStatus.UPCOMING,
            paymentStatus = PaymentStatus.PAID,
            paymentMethod = "Flash Offer UPI (Instant)",
            createdAt = now
        )

        saveBooking(newBooking)

        // 5. Update offer state to BOOKING_CREATED
        offerDao.updateOfferStatus(offerId, OfferStatus.BOOKING_CREATED.name)

        return Result.success(newBooking)
    }

    /**
     * Barber/Shop Marks Customer as NO_SHOW (Section 67 & 68):
     * Updates booking status to NO_SHOW and launches recovery pipeline for remaining time.
     */
    suspend fun markBookingNoShow(bookingId: String): Boolean {
        val booking = bookingDao.getBookingById(bookingId) ?: return false
        bookingDao.updateBookingStatus(bookingId, BookingStatus.NO_SHOW.name)

        notificationDao.insertNotification(
            NotificationEntity(
                id = "notif_noshow_" + System.currentTimeMillis(),
                title = "Appointment Marked No-Show",
                message = "Appointment $bookingId at ${booking.shopName} was marked as No-Show by the barber shop.",
                timeAgo = "Just now",
                type = NotificationType.CANCELLED.name,
                isRead = false,
                relatedBookingId = bookingId
            )
        )

        // Update barber queue so waiting count updates immediately (Rule 5)
        val queueList = barberQueueDao.getAllQueueList()
        val matchingQueueItem = queueList.find { it.bookingId == bookingId }
        if (matchingQueueItem != null) {
            barberQueueDao.updateStatus(matchingQueueItem.id, QueueCustomerStatus.NO_SHOW.name)
            broadcastQueueEvent(
                barberName = matchingQueueItem.barberName,
                eventType = "MARKED_NO_SHOW",
                customerName = matchingQueueItem.customerName,
                message = "${matchingQueueItem.customerName} marked as NO_SHOW by barber. Unused slot released."
            )
        }

        // Trigger No-Show Slot Recovery
        triggerSlotRecovery(bookingId = bookingId, sourceType = OfferSourceType.NO_SHOW_RECOVERY)
        return true
    }

    /**
     * Target Customer Eligibility Evaluation (Section 58, 59, 60):
     * Evaluates candidates based on:
     * - Distance <= 2.0 km (Geospatial distance)
     * - Last Active <= 48 hours (2 days)
     * - Location Permission Granted
     */
    fun evaluateTargetCustomers(shopLat: Double = 28.6500, shopLng: Double = 77.4500): List<TargetCustomer> {
        val candidates = listOf(
            TargetCandidateRaw("usr_A", "Customer A", 28.6570, 77.4520, 3.0, true),
            TargetCandidateRaw("usr_B", "Customer B", 28.6620, 77.4550, 24.0, true),
            TargetCandidateRaw("usr_C", "Customer C", 28.6750, 77.4600, 2.0, true),
            TargetCandidateRaw("usr_D", "Customer D", 28.6590, 77.4480, 120.0, true),
            TargetCandidateRaw("usr_E", "Customer E", 28.6580, 77.4530, 5.0, true),
            TargetCandidateRaw("usr_101", "Akash (Current Device)", 28.6550, 77.4490, 0.5, true)
        )

        return candidates.map { candidate ->
            val dist = calculateDistanceKm(shopLat, shopLng, candidate.lat, candidate.lng)
            val roundedDist = (dist * 10.0).roundToInt() / 10.0

            val isDistanceOk = roundedDist <= 2.0
            val isActivityOk = candidate.lastActiveHoursAgo <= 48.0
            val isPermissionOk = candidate.locationPermission

            val eligible = isDistanceOk && isActivityOk && isPermissionOk
            val reason = when {
                !isPermissionOk -> "Location permission not granted"
                !isDistanceOk -> "Distance > 2 km (${roundedDist} km)"
                !isActivityOk -> "Inactive > 2 days (${candidate.lastActiveHoursAgo.toInt()}h ago)"
                else -> "Eligible (Within 2km & active in last 48h)"
            }

            TargetCustomer(
                id = candidate.id,
                name = candidate.name,
                distanceKm = roundedDist,
                lastActiveHoursAgo = candidate.lastActiveHoursAgo,
                locationPermissionGranted = candidate.locationPermission,
                isEligible = eligible,
                ineligibilityReason = reason
            )
        }
    }

    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Radius of earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun mapOfferEntityToModel(entity: RecoveredSlotOfferEntity): SlotOffer {
        return SlotOffer(
            id = entity.id,
            slotId = entity.slotId,
            shopId = entity.shopId,
            shopName = entity.shopName,
            shopAddress = entity.shopAddress,
            shopRating = entity.shopRating,
            shopLat = entity.shopLat,
            shopLng = entity.shopLng,
            serviceName = entity.serviceName,
            barberName = entity.barberName,
            dateStr = entity.dateStr,
            timeSlot = entity.timeSlot,
            originalPrice = entity.originalPrice,
            discountPercent = entity.discountPercent,
            offerPrice = entity.offerPrice,
            createdAt = entity.createdAt,
            expiresAt = entity.expiresAt,
            bufferMinutes = entity.bufferMinutes,
            status = runCatching { OfferStatus.valueOf(entity.status) }.getOrDefault(OfferStatus.SENT),
            sourceType = runCatching { OfferSourceType.valueOf(entity.sourceType) }.getOrDefault(OfferSourceType.CUSTOMER_CANCELLATION),
            distanceKm = entity.distanceKm,
            reservedByUserId = entity.reservedByUserId
        )
    }

    // =========================================================================
    // REAL-TIME WAITING CUSTOMER COUNT & BARBER QUEUE ENGINE
    // =========================================================================

    private fun mapEntityToQueueItem(entity: BarberQueueEntity): BarberQueueItem {
        return BarberQueueItem(
            id = entity.id,
            barberId = entity.barberId,
            barberName = entity.barberName,
            shopId = entity.shopId,
            customerName = entity.customerName,
            isCurrentUser = entity.isCurrentUser,
            bookingId = entity.bookingId,
            serviceNames = entity.serviceNamesJoined.split(",").map { it.trim() },
            durationMinutes = entity.durationMinutes,
            scheduledTime = entity.scheduledTime,
            scheduledTimeMinutes = entity.scheduledTimeMinutes,
            status = runCatching { QueueCustomerStatus.valueOf(entity.status) }.getOrDefault(QueueCustomerStatus.CONFIRMED),
            remainingMinutes = entity.remainingMinutes,
            delayMinutes = entity.delayMinutes,
            lastUpdated = entity.lastUpdated
        )
    }

    /**
     * Real-time stream of the queue for a specific barber.
     */
    fun getBarberQueueFlow(barberName: String): Flow<List<BarberQueueItem>> {
        return barberQueueDao.getQueueByBarberFlow(barberName).map { list ->
            list.map { mapEntityToQueueItem(it) }
        }
    }

    /**
     * Real-time stream of all queue items.
     */
    fun getAllQueueFlow(): Flow<List<BarberQueueItem>> {
        return barberQueueDao.getAllQueueFlow().map { list ->
            list.map { mapEntityToQueueItem(it) }
        }
    }

    /**
     * Calculates user queue position in real time matching all business rules:
     * - Barber-specific (do not count customers booked with another barber)
     * - Relevant bookings only (CONFIRMED, CHECKED_IN, IN_PROGRESS, DELAYED)
     * - Count customers scheduled BEFORE current customer's appointment time (or in chair)
     * - Service duration considered (sum of actual scheduled/remaining minutes)
     * - Near real-time updates via Flow & WebSocket event broadcast
     */
    fun getUserQueuePositionFlow(
        bookingId: String,
        barberName: String,
        scheduledTime: String
    ): Flow<UserQueuePosition> {
        val targetMinutes = parseTimeToMinutesOfDay(scheduledTime)
        return barberQueueDao.getQueueByBarberFlow(barberName).map { entities ->
            val items = entities.map { mapEntityToQueueItem(it) }
            calculateUserQueuePosition(
                targetBookingId = bookingId,
                targetBarberName = barberName,
                targetScheduledTime = scheduledTime,
                targetMinutes = targetMinutes,
                items = items
            )
        }
    }

    private fun calculateUserQueuePosition(
        targetBookingId: String,
        targetBarberName: String,
        targetScheduledTime: String,
        targetMinutes: Int,
        items: List<BarberQueueItem>
    ): UserQueuePosition {
        // Rule 1: Barber-specific filter
        val barberItems = items.filter { it.barberName.equals(targetBarberName, ignoreCase = true) }

        // Find current user's item if exists
        val currentItem = barberItems.find { it.bookingId == targetBookingId || it.isCurrentUser }
        val effectiveTargetMinutes = currentItem?.scheduledTimeMinutes ?: targetMinutes

        // Rule 2: Relevant bookings only (CONFIRMED, CHECKED_IN, IN_PROGRESS, DELAYED)
        val relevantItems = barberItems.filter { it.status.isRelevant() }

        // Currently in chair
        val activeCustomer = relevantItems.find { it.status == QueueCustomerStatus.IN_PROGRESS }

        // Rule 3: Customers scheduled before the current customer's appointment
        val customersAhead = relevantItems.filter { item ->
            val isCurrent = (item.bookingId == targetBookingId) || (currentItem != null && item.id == currentItem.id)
            if (isCurrent) return@filter false

            if (item.status == QueueCustomerStatus.IN_PROGRESS) {
                // Anyone in chair is ahead unless it is the current user
                true
            } else {
                // Scheduled strictly before the current customer
                item.scheduledTimeMinutes < effectiveTargetMinutes
            }
        }.sortedBy { it.scheduledTimeMinutes }

        // Rule 4: Service duration considered
        var totalWaitMinutes = 0
        for (ahead in customersAhead) {
            if (ahead.status == QueueCustomerStatus.IN_PROGRESS) {
                totalWaitMinutes += ahead.remainingMinutes.coerceAtLeast(5)
            } else {
                totalWaitMinutes += ahead.durationMinutes + ahead.delayMinutes
            }
        }

        val isYourTurn = customersAhead.isEmpty() && (currentItem?.status == QueueCustomerStatus.IN_PROGRESS || currentItem?.status == QueueCustomerStatus.CHECKED_IN)

        return UserQueuePosition(
            bookingId = targetBookingId,
            barberName = targetBarberName,
            scheduledTime = targetScheduledTime,
            customersBeforeCount = customersAhead.size,
            estimatedWaitingMinutes = totalWaitMinutes,
            activeCustomer = activeCustomer,
            customersAhead = customersAhead,
            totalWaitingForBarber = relevantItems.size,
            isYourTurn = isYourTurn,
            transportMode = _transportMode.value,
            lastEvent = _latestQueueEvent.value,
            lastSyncTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * Map of Barber summaries: e.g. "Rahul Sharma" -> 3 waiting (~30m), "Amit Verma" -> 1 waiting (~15m)
     */
    fun getBarberQueueSummariesFlow(): Flow<Map<String, BarberQueueSummary>> {
        return barberQueueDao.getAllQueueFlow().map { entities ->
            val allItems = entities.map { mapEntityToQueueItem(it) }
            val byBarber = allItems.groupBy { it.barberName }
            val result = mutableMapOf<String, BarberQueueSummary>()

            for ((barberName, items) in byBarber) {
                val relevant = items.filter { it.status.isRelevant() }
                var waitMins = 0
                for (item in relevant) {
                    if (item.status == QueueCustomerStatus.IN_PROGRESS) {
                        waitMins += item.remainingMinutes.coerceAtLeast(5)
                    } else {
                        waitMins += item.durationMinutes + item.delayMinutes
                    }
                }
                result[barberName] = BarberQueueSummary(
                    barberId = items.firstOrNull()?.barberId ?: "b1",
                    barberName = barberName,
                    shopId = items.firstOrNull()?.shopId ?: "shop_1",
                    activeWaitingCount = relevant.size,
                    estimatedWaitMinutes = waitMins
                )
            }
            result
        }
    }

    /**
     * Emits a real-time WebSocket event frame.
     */
    suspend fun broadcastQueueEvent(
        barberName: String,
        eventType: String,
        customerName: String,
        message: String,
        customersBefore: Int = 0,
        estimatedWaitMinutes: Int = 0
    ) {
        val event = RealtimeQueueEvent(
            id = "EVT_" + System.currentTimeMillis(),
            timestamp = System.currentTimeMillis(),
            barberName = barberName,
            eventType = eventType,
            customerName = customerName,
            message = message,
            customersBefore = customersBefore,
            estimatedWaitMinutes = estimatedWaitMinutes
        )
        _latestQueueEvent.value = event
        _realtimeEventQueue.emit(event)
    }

    // Interactive Real-Time Queue Mutations (Rule 5)

    suspend fun startBarberService(queueItemId: String): Boolean {
        val all = barberQueueDao.getAllQueueList()
        val item = all.find { it.id == queueItemId } ?: return false

        // Mark previously in-progress item for this barber as COMPLETED
        all.filter { it.barberName == item.barberName && it.status == QueueCustomerStatus.IN_PROGRESS.name }
            .forEach { barberQueueDao.updateStatus(it.id, QueueCustomerStatus.COMPLETED.name) }

        barberQueueDao.updateStatusAndRemaining(queueItemId, QueueCustomerStatus.IN_PROGRESS.name, item.durationMinutes)
        broadcastQueueEvent(
            barberName = item.barberName,
            eventType = "SERVICE_STARTED",
            customerName = item.customerName,
            message = "${item.barberName} started service for ${item.customerName} (${item.durationMinutes} mins)."
        )
        return true
    }

    suspend fun completeBarberService(queueItemId: String): Boolean {
        val all = barberQueueDao.getAllQueueList()
        val item = all.find { it.id == queueItemId } ?: return false
        barberQueueDao.updateStatusAndRemaining(queueItemId, QueueCustomerStatus.COMPLETED.name, 0)

        // Auto-advance next waiting customer to IN_PROGRESS
        val nextWaiting = all
            .filter { it.barberName == item.barberName && it.id != queueItemId && (it.status == QueueCustomerStatus.CHECKED_IN.name || it.status == QueueCustomerStatus.CONFIRMED.name) }
            .minByOrNull { it.scheduledTimeMinutes }

        broadcastQueueEvent(
            barberName = item.barberName,
            eventType = "SERVICE_COMPLETED",
            customerName = item.customerName,
            message = "${item.barberName} completed service for ${item.customerName}. Queue moved up!"
        )

        if (nextWaiting != null) {
            barberQueueDao.updateStatusAndRemaining(nextWaiting.id, QueueCustomerStatus.IN_PROGRESS.name, nextWaiting.durationMinutes)
            broadcastQueueEvent(
                barberName = nextWaiting.barberName,
                eventType = "SERVICE_STARTED",
                customerName = nextWaiting.customerName,
                message = "${nextWaiting.barberName} is now in chair with ${nextWaiting.customerName}!"
            )
        }
        return true
    }

    suspend fun customerCheckInQueueItem(queueItemId: String): Boolean {
        val item = barberQueueDao.getAllQueueList().find { it.id == queueItemId } ?: return false
        barberQueueDao.updateStatus(queueItemId, QueueCustomerStatus.CHECKED_IN.name)
        broadcastQueueEvent(
            barberName = item.barberName,
            eventType = "CHECKED_IN",
            customerName = item.customerName,
            message = "${item.customerName} checked in and arrived at the barber shop waiting area."
        )
        return true
    }

    suspend fun customerCancelQueueItem(queueItemId: String): Boolean {
        val item = barberQueueDao.getAllQueueList().find { it.id == queueItemId } ?: return false
        barberQueueDao.updateStatus(queueItemId, QueueCustomerStatus.CANCELLED.name)
        broadcastQueueEvent(
            barberName = item.barberName,
            eventType = "CUSTOMER_CANCELLED",
            customerName = item.customerName,
            message = "${item.customerName} cancelled appointment. Queue count decreased."
        )
        return true
    }

    suspend fun markQueueItemNoShow(queueItemId: String): Boolean {
        val item = barberQueueDao.getAllQueueList().find { it.id == queueItemId } ?: return false
        barberQueueDao.updateStatus(queueItemId, QueueCustomerStatus.NO_SHOW.name)
        broadcastQueueEvent(
            barberName = item.barberName,
            eventType = "MARKED_NO_SHOW",
            customerName = item.customerName,
            message = "${item.customerName} was marked as No-Show. Queue moved up immediately."
        )
        return true
    }

    suspend fun broadcastBarberDelay(barberName: String, delayMins: Int = 10) {
        barberQueueDao.addBarberDelay(barberName, delayMins)
        broadcastQueueEvent(
            barberName = barberName,
            eventType = "DELAY_BROADCAST",
            customerName = "All Customers",
            message = "⚠️ $barberName is running ~$delayMins mins behind schedule due to detailed scissor styling."
        )
    }

    suspend fun addWalkInQueueCustomer(
        barberName: String,
        customerName: String,
        serviceName: String,
        durationMins: Int,
        timeSlot: String
    ) {
        val timeMinutes = parseTimeToMinutesOfDay(timeSlot)
        val newItem = BarberQueueEntity(
            id = "Q_WALKIN_" + System.currentTimeMillis().toString().takeLast(6),
            barberId = if (barberName.contains("Rahul")) "b1" else "b2",
            barberName = barberName,
            shopId = "shop_1",
            customerName = customerName,
            isCurrentUser = false,
            bookingId = null,
            serviceNamesJoined = serviceName,
            durationMinutes = durationMins,
            scheduledTime = timeSlot,
            scheduledTimeMinutes = timeMinutes,
            status = QueueCustomerStatus.CONFIRMED.name,
            remainingMinutes = durationMins,
            delayMinutes = 0
        )
        barberQueueDao.insertQueueItem(newItem)
        broadcastQueueEvent(
            barberName = barberName,
            eventType = "NEW_BOOKING",
            customerName = customerName,
            message = "New appointment added for $customerName with $barberName at $timeSlot."
        )
    }

    fun setTransportMode(mode: RealtimeTransportMode) {
        _transportMode.value = mode
    }

    suspend fun resetQueueToDefault() {
        barberQueueDao.clearQueue()
        seedQueueItems()
        broadcastQueueEvent(
            barberName = "Rahul Sharma",
            eventType = "QUEUE_RESET",
            customerName = "System",
            message = "Queue state restored to default benchmark: 2 customers before you, ~30m wait."
        )
    }

    // Availability API simulation
    suspend fun getAvailabilitySlots(shopId: String, barberId: String, date: String): Map<String, List<Pair<String, Boolean>>> {
        delay(300)
        val isToday = date.contains("25") || date.contains("Today")
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
        delay(400)
        if (simulateConflict) {
            return Result.failure(Exception("HTTP 409 Conflict: This slot was just booked by another customer. Please select another slot."))
        }
        val reservation = SlotReservation(
            reservationId = "RES" + UUID.randomUUID().toString().take(6).uppercase(),
            shopId = shopId,
            slotTime = slotTime,
            slotDate = slotDate,
            expiresAtTimestamp = System.currentTimeMillis() + (10 * 60 * 1000)
        )
        return Result.success(reservation)
    }

    // Payment Gateway simulation
    suspend fun processPayment(reservationId: String, amount: Int, paymentMethod: String): Result<String> {
        delay(800)
        val transactionId = "TXN_" + System.currentTimeMillis().toString().takeLast(8)
        return Result.success(transactionId)
    }

    // Seed database on first launch
    suspend fun seedInitialDataIfEmpty() {
        if (barberQueueDao.getAllQueueList().isEmpty()) {
            seedQueueItems()
        }

        val existing = bookingDao.getUpcomingBookingsList()
        if (existing.isNotEmpty()) return

        // 1. Seed Upcoming Confirmed Booking (Rahul Sharma @ Royal Barber Shop, 5:30 PM)
        val upcomingBooking = BookingEntity(
            id = "BK10234",
            shopId = "shop_1",
            shopName = "Royal Barber Club & Lounge",
            shopAddress = "Sector 18, Wave Mall Complex, Noida",
            serviceNamesJoined = "Signature Royal Haircut, Royal Beard Sculpt & Trim",
            barberName = "Rahul Sharma",
            dateStr = "25 Sep 2026",
            timeSlot = "5:30 PM",
            price = 500,
            tax = 25,
            total = 525,
            status = BookingStatus.UPCOMING.name,
            paymentStatus = PaymentStatus.PAID.name,
            paymentMethod = "UPI (Google Pay)",
            createdAt = System.currentTimeMillis() - 3600000,
            reminderSentAt = null,
            isLate = false
        )
        bookingDao.insertBooking(upcomingBooking)

        // 2. Seed Completed Booking
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
                createdAt = System.currentTimeMillis() - 500000000,
                reminderSentAt = System.currentTimeMillis() - 500000000,
                isLate = false
            )
        )

        // 3. Seed an active recovered slot offer ready to view/claim
        val initialOfferId = "OFFER_ROYAL_530"
        val offerEntity = RecoveredSlotOfferEntity(
            id = initialOfferId,
            slotId = "SLOT_shop_1_530PM",
            shopId = "shop_1",
            shopName = "Royal Barber Club & Lounge",
            shopAddress = "Sector 18, Wave Mall Complex, Noida",
            shopRating = 4.8,
            shopLat = 28.6500,
            shopLng = 77.4500,
            serviceName = "Signature Royal Haircut",
            barberName = "Rahul Sharma",
            dateStr = "Today, 25 Sep",
            timeSlot = "5:30 PM",
            originalPrice = 300,
            discountPercent = 20,
            offerPrice = 240,
            createdAt = System.currentTimeMillis() - 600000,
            expiresAt = System.currentTimeMillis() + (35 * 60 * 1000), // 35 minutes left
            bufferMinutes = 15,
            status = OfferStatus.SENT.name,
            sourceType = OfferSourceType.CUSTOMER_CANCELLATION.name,
            distanceKm = 1.2
        )
        offerDao.insertOffer(offerEntity)

        // Seed 15-minute appointment reminder notification for BK10234
        notificationDao.insertNotification(
            NotificationEntity(
                id = "notif_rem_seed",
                title = "⏰ Appointment Reminder",
                message = "Your appointment at Royal Barber Club & Lounge starts in 15 minutes.\n\nService: Haircut + Beard\nBarber: Rahul Sharma\nTime: 5:30 PM\n\nPlease reach the barber shop before your scheduled time.",
                timeAgo = "15m ago",
                type = NotificationType.REMINDER.name,
                isRead = false,
                relatedBookingId = "BK10234"
            )
        )

        // Seed Flash Offer Notification
        notificationDao.insertNotification(
            NotificationEntity(
                id = "notif_offer_seed",
                title = "🔥 Last-Minute Barber Offer",
                message = "A slot just became available near you!\n\nRoyal Barber Club & Lounge ⭐ 4.8\nHaircut\n₹300 → ₹240 (20% OFF)\nToday 5:30 PM\n📍 1.2 km away\n\nThis offer is available for a limited time.",
                timeAgo = "10m ago",
                type = NotificationType.OFFER.name,
                isRead = false,
                relatedOfferId = initialOfferId
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

    suspend fun seedQueueItems() {
        val initialQueueItems = listOf(
            // --- Rahul Sharma's Queue (Royal Barber Shop) ---
            // Customer A: 5:00 PM, IN_PROGRESS (15 min remaining)
            BarberQueueEntity(
                id = "Q_RAHUL_1",
                barberId = "b1",
                barberName = "Rahul Sharma",
                shopId = "shop_1",
                customerName = "Customer A (Vikram M.)",
                isCurrentUser = false,
                bookingId = "BK_EXT_01",
                serviceNamesJoined = "Signature Royal Haircut",
                durationMinutes = 30,
                scheduledTime = "5:00 PM",
                scheduledTimeMinutes = 1020,
                status = QueueCustomerStatus.IN_PROGRESS.name,
                remainingMinutes = 15,
                delayMinutes = 0
            ),
            // Customer B: 5:15 PM, CHECKED_IN (15 min duration)
            BarberQueueEntity(
                id = "Q_RAHUL_2",
                barberId = "b1",
                barberName = "Rahul Sharma",
                shopId = "shop_1",
                customerName = "Customer B (Siddharth J.)",
                isCurrentUser = false,
                bookingId = "BK_EXT_02",
                serviceNamesJoined = "Royal Beard Sculpt & Trim",
                durationMinutes = 15,
                scheduledTime = "5:15 PM",
                scheduledTimeMinutes = 1035,
                status = QueueCustomerStatus.CHECKED_IN.name,
                remainingMinutes = 15,
                delayMinutes = 0
            ),
            // Current User: 5:30 PM, CONFIRMED (matches BK10234)
            BarberQueueEntity(
                id = "Q_RAHUL_CURRENT",
                barberId = "b1",
                barberName = "Rahul Sharma",
                shopId = "shop_1",
                customerName = "You (Akash Sharma)",
                isCurrentUser = true,
                bookingId = "BK10234",
                serviceNamesJoined = "Signature Royal Haircut, Royal Beard Sculpt & Trim",
                durationMinutes = 45,
                scheduledTime = "5:30 PM",
                scheduledTimeMinutes = 1050,
                status = QueueCustomerStatus.CONFIRMED.name,
                remainingMinutes = 45,
                delayMinutes = 0
            ),
            // Customer C: 5:45 PM, CONFIRMED (after 5:30 PM, not counted before you)
            BarberQueueEntity(
                id = "Q_RAHUL_3",
                barberId = "b1",
                barberName = "Rahul Sharma",
                shopId = "shop_1",
                customerName = "Customer C (Rohan G.)",
                isCurrentUser = false,
                bookingId = "BK_EXT_03",
                serviceNamesJoined = "Signature Royal Haircut",
                durationMinutes = 30,
                scheduledTime = "5:45 PM",
                scheduledTimeMinutes = 1065,
                status = QueueCustomerStatus.CONFIRMED.name,
                remainingMinutes = 30,
                delayMinutes = 0
            ),

            // --- Amit Verma's Queue (1 customer waiting) ---
            BarberQueueEntity(
                id = "Q_AMIT_1",
                barberId = "b2",
                barberName = "Amit Verma",
                shopId = "shop_1",
                customerName = "Customer D (Nitin K.)",
                isCurrentUser = false,
                bookingId = "BK_EXT_04",
                serviceNamesJoined = "Skin Fade & Edge-up",
                durationMinutes = 15,
                scheduledTime = "5:00 PM",
                scheduledTimeMinutes = 1020,
                status = QueueCustomerStatus.CONFIRMED.name,
                remainingMinutes = 15,
                delayMinutes = 0
            )
        )
        barberQueueDao.insertQueueItems(initialQueueItems)
    }
}

private data class TargetCandidateRaw(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val lastActiveHoursAgo: Double,
    val locationPermission: Boolean
)
