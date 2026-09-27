package com.example.data.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val token: String = "token_jwt_sample_xyz",
    val refreshToken: String = "refresh_sample_abc"
)

enum class BookingStatus {
    UPCOMING,
    COMPLETED,
    CANCELLED,
    NO_SHOW
}

enum class OfferStatus {
    CREATED,
    SENT,
    VIEWED,
    ACCEPTED,
    BOOKING_CREATED,
    EXPIRED,
    REJECTED
}

enum class OfferSourceType {
    CUSTOMER_CANCELLATION,
    NO_SHOW_RECOVERY
}

data class SlotOffer(
    val id: String,
    val slotId: String,
    val shopId: String,
    val shopName: String,
    val shopAddress: String,
    val shopRating: Double,
    val shopLat: Double = 28.6500,
    val shopLng: Double = 77.4500,
    val serviceName: String,
    val barberName: String,
    val dateStr: String,
    val timeSlot: String,
    val originalPrice: Int,
    val discountPercent: Int = 20,
    val offerPrice: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long,
    val bufferMinutes: Int = 15,
    val status: OfferStatus = OfferStatus.SENT,
    val sourceType: OfferSourceType = OfferSourceType.CUSTOMER_CANCELLATION,
    val distanceKm: Double = 1.2,
    val reservedByUserId: String? = null
)

data class TargetCustomer(
    val id: String,
    val name: String,
    val distanceKm: Double,
    val lastActiveHoursAgo: Double,
    val locationPermissionGranted: Boolean = true,
    val isEligible: Boolean = false,
    val ineligibilityReason: String = ""
)

enum class PaymentStatus {
    PAID,
    PENDING,
    REFUNDED
}

data class BarberService(
    val id: String,
    val name: String,
    val category: String, // "Haircut", "Beard", "Hair Color", "Facial", "Hair Spa", "Kids Haircut"
    val price: Int,
    val durationMin: Int,
    val description: String = ""
)

data class Barber(
    val id: String,
    val name: String,
    val rating: Double,
    val experienceYears: Int,
    val specialty: String,
    val isAvailable: Boolean = true
)

data class Review(
    val id: String,
    val userName: String,
    val rating: Double,
    val comment: String,
    val date: String,
    val barberName: String = ""
)

data class BarberShop(
    val id: String,
    val name: String,
    val rating: Double,
    val reviewCount: Int,
    val distanceKm: Double,
    val startingPrice: Int,
    val nextAvailableSlot: String,
    val isOpen: Boolean,
    val address: String,
    val city: String,
    val imageDrawableRes: Int,
    val description: String,
    val services: List<BarberService>,
    val barbers: List<Barber>,
    val reviews: List<Review>,
    val isFavorite: Boolean = false,
    val phone: String = "+91 98765 43210"
)

data class Booking(
    val id: String,
    val shopId: String,
    val shopName: String,
    val shopAddress: String,
    val serviceNames: List<String>,
    val barberName: String,
    val dateStr: String,
    val timeSlot: String,
    val price: Int,
    val tax: Int,
    val total: Int,
    val status: BookingStatus = BookingStatus.UPCOMING,
    val paymentStatus: PaymentStatus = PaymentStatus.PAID,
    val paymentMethod: String = "UPI (GPay)",
    val createdAt: Long = System.currentTimeMillis(),
    val reminderSentAt: Long? = null,
    val isLate: Boolean = false
)

data class SlotReservation(
    val reservationId: String,
    val shopId: String,
    val slotTime: String,
    val slotDate: String,
    val expiresAtTimestamp: Long
)

enum class NotificationType {
    CONFIRMED,
    REMINDER,
    CANCELLED,
    RESCHEDULED,
    OFFER
}

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timeAgo: String,
    val type: NotificationType,
    val isRead: Boolean = false,
    val relatedBookingId: String? = null,
    val relatedOfferId: String? = null
)

enum class SortOption {
    RECOMMENDED,
    NEAREST,
    HIGHEST_RATED,
    LOWEST_PRICE
}

data class FilterCriteria(
    val maxDistanceKm: Double = 10.0,
    val minRating: Double = 0.0,
    val maxPrice: Int = 1500,
    val selectedCategories: Set<String> = emptySet(),
    val openNowOnly: Boolean = false,
    val availableTodayOnly: Boolean = false,
    val sortBy: SortOption = SortOption.RECOMMENDED
)

// =========================================================================
// REAL-TIME WAITING CUSTOMER COUNT & BARBER QUEUE MODELS
// =========================================================================

enum class QueueCustomerStatus {
    CONFIRMED,
    CHECKED_IN,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    NO_SHOW,
    DELAYED,
    EXPIRED;

    fun isRelevant(): Boolean {
        return when (this) {
            CONFIRMED, CHECKED_IN, IN_PROGRESS, DELAYED -> true
            COMPLETED, CANCELLED, NO_SHOW, EXPIRED -> false
        }
    }
}

data class BarberQueueItem(
    val id: String,
    val barberId: String,
    val barberName: String,
    val shopId: String,
    val customerName: String,
    val isCurrentUser: Boolean = false,
    val bookingId: String? = null,
    val serviceNames: List<String>,
    val durationMinutes: Int,
    val scheduledTime: String,
    val scheduledTimeMinutes: Int,
    val status: QueueCustomerStatus,
    val remainingMinutes: Int,
    val delayMinutes: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class BarberQueueSummary(
    val barberId: String,
    val barberName: String,
    val shopId: String,
    val activeWaitingCount: Int,
    val estimatedWaitMinutes: Int
)

enum class RealtimeTransportMode(val label: String, val badgeColor: Long) {
    WEBSOCKET("WebSocket Live Push", 0xFF10B981), // Emerald Green
    SSE("SSE Streaming Channel", 0xFF3B82F6),     // Electric Blue
    POLLING("REST API Polling (3s)", 0xFFF59E0B) // Amber
}

data class RealtimeQueueEvent(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val barberName: String,
    val eventType: String,
    val customerName: String,
    val message: String,
    val customersBefore: Int,
    val estimatedWaitMinutes: Int
)

data class UserQueuePosition(
    val bookingId: String,
    val barberName: String,
    val scheduledTime: String,
    val customersBeforeCount: Int,
    val estimatedWaitingMinutes: Int,
    val activeCustomer: BarberQueueItem?,
    val customersAhead: List<BarberQueueItem>,
    val totalWaitingForBarber: Int,
    val isYourTurn: Boolean = false,
    val transportMode: RealtimeTransportMode = RealtimeTransportMode.WEBSOCKET,
    val lastEvent: RealtimeQueueEvent? = null,
    val lastSyncTimestamp: Long = System.currentTimeMillis()
)

fun parseTimeToMinutesOfDay(timeStr: String): Int {
    val clean = timeStr.trim().uppercase()
    val isPm = clean.contains("PM")
    val timePart = clean.replace("AM", "").replace("PM", "").trim()
    val parts = timePart.split(":")
    if (parts.isEmpty()) return 0
    var hours = parts[0].toIntOrNull() ?: 0
    val minutes = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
    if (isPm && hours < 12) hours += 12
    if (!isPm && hours == 12) hours = 0
    return hours * 60 + minutes
}
