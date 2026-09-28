package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "message") val message: String? = null,
    @Json(name = "count") val count: Int? = null,
    @Json(name = "data") val data: T? = null,
    @Json(name = "status") val status: String? = null
)

@JsonClass(generateAdapter = true)
data class HealthResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "service") val service: String? = null,
    @Json(name = "timestamp") val timestamp: String? = null
)

@JsonClass(generateAdapter = true)
data class ShopDto(
    @Json(name = "shopId") val shopId: String? = null,
    @Json(name = "_id") val id: String? = null,
    @Json(name = "name") val name: String = "",
    @Json(name = "rating") val rating: Double = 4.5,
    @Json(name = "reviewCount") val reviewCount: Int = 100,
    @Json(name = "distanceKm") val distanceKm: Double = 1.0,
    @Json(name = "startingPrice") val startingPrice: Int = 200,
    @Json(name = "nextAvailableSlot") val nextAvailableSlot: String = "5:30 PM",
    @Json(name = "isOpen") val isOpen: Boolean = true,
    @Json(name = "address") val address: String = "",
    @Json(name = "city") val city: String = "Noida, Uttar Pradesh",
    @Json(name = "description") val description: String = "",
    @Json(name = "phone") val phone: String = "+91 98765 43210",
    @Json(name = "services") val services: List<ServiceDto>? = null,
    @Json(name = "barbers") val barbers: List<BarberDto>? = null,
    @Json(name = "reviews") val reviews: List<ReviewDto>? = null
)

@JsonClass(generateAdapter = true)
data class ServiceDto(
    @Json(name = "serviceId") val serviceId: String? = null,
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String = "",
    @Json(name = "category") val category: String = "Haircut",
    @Json(name = "price") val price: Int = 250,
    @Json(name = "durationMin") val durationMin: Int = 30,
    @Json(name = "description") val description: String = ""
)

@JsonClass(generateAdapter = true)
data class BarberDto(
    @Json(name = "barberId") val barberId: String? = null,
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String = "",
    @Json(name = "rating") val rating: Double = 4.8,
    @Json(name = "experienceYears") val experienceYears: Int = 5,
    @Json(name = "specialty") val specialty: String = "Stylist",
    @Json(name = "isAvailable") val isAvailable: Boolean = true
)

@JsonClass(generateAdapter = true)
data class ReviewDto(
    @Json(name = "reviewId") val reviewId: String? = null,
    @Json(name = "id") val id: String? = null,
    @Json(name = "userName") val userName: String = "Customer",
    @Json(name = "rating") val rating: Double = 5.0,
    @Json(name = "comment") val comment: String = "",
    @Json(name = "date") val date: String = "Recently",
    @Json(name = "barberName") val barberName: String = ""
)

@JsonClass(generateAdapter = true)
data class BarberSummaryDto(
    @Json(name = "barberId") val barberId: String = "",
    @Json(name = "barberName") val barberName: String = "",
    @Json(name = "shopId") val shopId: String = "",
    @Json(name = "activeWaitingCount") val activeWaitingCount: Int = 0,
    @Json(name = "estimatedWaitMinutes") val estimatedWaitMinutes: Int = 0
)

@JsonClass(generateAdapter = true)
data class QueuePositionDto(
    @Json(name = "bookingId") val bookingId: String = "",
    @Json(name = "barberName") val barberName: String = "",
    @Json(name = "scheduledTime") val scheduledTime: String = "",
    @Json(name = "customersBeforeCount") val customersBeforeCount: Int = 0,
    @Json(name = "estimatedWaitingMinutes") val estimatedWaitingMinutes: Int = 0,
    @Json(name = "activeCustomer") val activeCustomer: QueueItemDto? = null,
    @Json(name = "customersAhead") val customersAhead: List<QueueItemDto> = emptyList(),
    @Json(name = "totalWaitingForBarber") val totalWaitingForBarber: Int = 0,
    @Json(name = "isYourTurn") val isYourTurn: Boolean = false,
    @Json(name = "lastSyncTimestamp") val lastSyncTimestamp: Long = 0
)

@JsonClass(generateAdapter = true)
data class QueueItemDto(
    @Json(name = "queueId") val queueId: String? = null,
    @Json(name = "_id") val id: String? = null,
    @Json(name = "barberId") val barberId: String = "",
    @Json(name = "barberName") val barberName: String = "",
    @Json(name = "shopId") val shopId: String = "",
    @Json(name = "customerName") val customerName: String = "",
    @Json(name = "isCurrentUser") val isCurrentUser: Boolean = false,
    @Json(name = "bookingId") val bookingId: String? = null,
    @Json(name = "serviceNames") val serviceNames: List<String> = emptyList(),
    @Json(name = "durationMinutes") val durationMinutes: Int = 30,
    @Json(name = "scheduledTime") val scheduledTime: String = "",
    @Json(name = "scheduledTimeMinutes") val scheduledTimeMinutes: Int = 0,
    @Json(name = "status") val status: String = "CONFIRMED",
    @Json(name = "remainingMinutes") val remainingMinutes: Int = 0,
    @Json(name = "delayMinutes") val delayMinutes: Int = 0
)

@JsonClass(generateAdapter = true)
data class OfferDto(
    @Json(name = "_id") val id: String? = null,
    @Json(name = "offerCode") val offerCode: String = "",
    @Json(name = "slotId") val slotId: String = "",
    @Json(name = "shopId") val shopId: String = "",
    @Json(name = "shopName") val shopName: String = "",
    @Json(name = "shopAddress") val shopAddress: String = "",
    @Json(name = "shopRating") val shopRating: Double = 4.8,
    @Json(name = "shopLat") val shopLat: Double = 28.65,
    @Json(name = "shopLng") val shopLng: Double = 77.45,
    @Json(name = "serviceName") val serviceName: String = "",
    @Json(name = "barberName") val barberName: String = "",
    @Json(name = "dateStr") val dateStr: String = "",
    @Json(name = "timeSlot") val timeSlot: String = "",
    @Json(name = "originalPrice") val originalPrice: Int = 300,
    @Json(name = "discountPercent") val discountPercent: Int = 20,
    @Json(name = "offerPrice") val offerPrice: Int = 240,
    @Json(name = "expiresAt") val expiresAt: String? = null,
    @Json(name = "bufferMinutes") val bufferMinutes: Int = 15,
    @Json(name = "status") val status: String = "SENT",
    @Json(name = "sourceType") val sourceType: String = "CUSTOMER_CANCELLATION",
    @Json(name = "distanceKm") val distanceKm: Double = 1.2
)

@JsonClass(generateAdapter = true)
data class HoldSlotRequest(
    @Json(name = "shopId") val shopId: String,
    @Json(name = "barberName") val barberName: String,
    @Json(name = "slotTime") val slotTime: String,
    @Json(name = "slotDate") val slotDate: String,
    @Json(name = "simulateConflict") val simulateConflict: Boolean = false
)

@JsonClass(generateAdapter = true)
data class HoldSlotResponse(
    @Json(name = "reservationId") val reservationId: String,
    @Json(name = "shopId") val shopId: String,
    @Json(name = "slotTime") val slotTime: String,
    @Json(name = "slotDate") val slotDate: String,
    @Json(name = "expiresAtTimestamp") val expiresAtTimestamp: Long? = null
)

@JsonClass(generateAdapter = true)
data class CreateBookingRequest(
    @Json(name = "bookingNumber") val bookingNumber: String,
    @Json(name = "shopId") val shopId: String,
    @Json(name = "shopName") val shopName: String,
    @Json(name = "shopAddress") val shopAddress: String,
    @Json(name = "serviceNames") val serviceNames: List<String>,
    @Json(name = "barberName") val barberName: String,
    @Json(name = "dateStr") val dateStr: String,
    @Json(name = "timeSlot") val timeSlot: String,
    @Json(name = "durationMinutes") val durationMinutes: Int = 30,
    @Json(name = "price") val price: Int,
    @Json(name = "tax") val tax: Int,
    @Json(name = "total") val total: Int,
    @Json(name = "paymentMethod") val paymentMethod: String = "UPI (GPay)"
)

@JsonClass(generateAdapter = true)
data class BookingDto(
    @Json(name = "bookingNumber") val bookingNumber: String? = null,
    @Json(name = "_id") val id: String? = null,
    @Json(name = "shopId") val shopId: String = "",
    @Json(name = "shopName") val shopName: String = "",
    @Json(name = "shopAddress") val shopAddress: String = "",
    @Json(name = "serviceNames") val serviceNames: List<String> = emptyList(),
    @Json(name = "barberName") val barberName: String = "",
    @Json(name = "dateStr") val dateStr: String = "",
    @Json(name = "timeSlot") val timeSlot: String = "",
    @Json(name = "price") val price: Int = 0,
    @Json(name = "tax") val tax: Int = 0,
    @Json(name = "total") val total: Int = 0,
    @Json(name = "status") val status: String = "UPCOMING",
    @Json(name = "paymentStatus") val paymentStatus: String = "PAID",
    @Json(name = "paymentMethod") val paymentMethod: String = "UPI",
    @Json(name = "isLate") val isLate: Boolean = false
)

@JsonClass(generateAdapter = true)
data class RescheduleRequest(
    @Json(name = "newDate") val newDate: String,
    @Json(name = "newTime") val newTime: String
)

@JsonClass(generateAdapter = true)
data class ClaimOfferResponse(
    @Json(name = "booking") val booking: BookingDto? = null,
    @Json(name = "offer") val offer: OfferDto? = null
)

@JsonClass(generateAdapter = true)
data class NotificationDto(
    @Json(name = "_id") val id: String? = null,
    @Json(name = "title") val title: String = "",
    @Json(name = "message") val message: String = "",
    @Json(name = "type") val type: String = "CONFIRMED",
    @Json(name = "isRead") val isRead: Boolean = false,
    @Json(name = "relatedBookingId") val relatedBookingId: String? = null,
    @Json(name = "relatedOfferId") val relatedOfferId: String? = null,
    @Json(name = "createdAt") val createdAt: String? = null
)
