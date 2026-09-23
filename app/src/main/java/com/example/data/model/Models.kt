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
    CANCELLED
}

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
    val createdAt: Long = System.currentTimeMillis()
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
    val isRead: Boolean = false
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
