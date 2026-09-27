package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val shopName: String,
    val shopAddress: String,
    val serviceNamesJoined: String, // comma separated
    val barberName: String,
    val dateStr: String,
    val timeSlot: String,
    val price: Int,
    val tax: Int,
    val total: Int,
    val status: String, // UPCOMING, COMPLETED, CANCELLED, NO_SHOW
    val paymentStatus: String, // PAID, PENDING, REFUNDED
    val paymentMethod: String,
    val createdAt: Long,
    val reminderSentAt: Long? = null,
    val isLate: Boolean = false
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val shopId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val timeAgo: String,
    val type: String, // CONFIRMED, REMINDER, CANCELLED, RESCHEDULED, OFFER
    val isRead: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val relatedBookingId: String? = null,
    val relatedOfferId: String? = null
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val phone: String
)

@Entity(tableName = "recovered_slot_offers")
data class RecoveredSlotOfferEntity(
    @PrimaryKey val id: String,
    val slotId: String,
    val shopId: String,
    val shopName: String,
    val shopAddress: String,
    val shopRating: Double,
    val shopLat: Double,
    val shopLng: Double,
    val serviceName: String,
    val barberName: String,
    val dateStr: String,
    val timeSlot: String,
    val originalPrice: Int,
    val discountPercent: Int,
    val offerPrice: Int,
    val createdAt: Long,
    val expiresAt: Long,
    val bufferMinutes: Int,
    val status: String, // CREATED, SENT, VIEWED, ACCEPTED, BOOKING_CREATED, EXPIRED, REJECTED
    val sourceType: String, // CUSTOMER_CANCELLATION, NO_SHOW_RECOVERY
    val distanceKm: Double,
    val reservedByUserId: String? = null
)

@Entity(tableName = "offer_delivery_records")
data class OfferDeliveryRecordEntity(
    @PrimaryKey val id: String,
    val offerId: String,
    val userId: String,
    val shopId: String,
    val timeSlot: String,
    val dateStr: String,
    val sentAt: Long
)

@Entity(tableName = "reminder_records")
data class ReminderRecordEntity(
    @PrimaryKey val id: String,
    val bookingId: String,
    val reminderSentAt: Long
)

@Entity(tableName = "barber_queue")
data class BarberQueueEntity(
    @PrimaryKey val id: String,
    val barberId: String,
    val barberName: String,
    val shopId: String,
    val customerName: String,
    val isCurrentUser: Boolean = false,
    val bookingId: String? = null,
    val serviceNamesJoined: String,
    val durationMinutes: Int,
    val scheduledTime: String,
    val scheduledTimeMinutes: Int,
    val status: String, // CONFIRMED, CHECKED_IN, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW, DELAYED, EXPIRED
    val remainingMinutes: Int,
    val delayMinutes: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)
