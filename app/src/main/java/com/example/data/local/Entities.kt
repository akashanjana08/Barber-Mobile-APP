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
    val status: String, // UPCOMING, COMPLETED, CANCELLED
    val paymentStatus: String, // PAID, PENDING, REFUNDED
    val paymentMethod: String,
    val createdAt: Long
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
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val phone: String
)
