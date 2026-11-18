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
    @Json(name = "id") val altId: String? = null,
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
    @Json(name = "_id") val mongoId: String? = null,
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
    @Json(name = "_id") val mongoId: String? = null,
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
    @Json(name = "barberId") val barberId: String,
    @Json(name = "barberName") val barberName: String,
    @Json(name = "startTime") val startTime: String,
    @Json(name = "bookingDate") val bookingDate: String,
    @Json(name = "slotTime") val slotTime: String,
    @Json(name = "slotDate") val slotDate: String,
    @Json(name = "durationMinutes") val durationMinutes: Int = 45,
    @Json(name = "serviceIds") val serviceIds: List<String> = emptyList(),
    @Json(name = "serviceNames") val serviceNames: List<String> = emptyList(),
    @Json(name = "simulateConflict") val simulateConflict: Boolean = false
)

@JsonClass(generateAdapter = true)
data class HoldSlotResponseDto(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "message") val message: String? = null,
    @Json(name = "count") val count: Int? = 0,
    @Json(name = "data") val data: Any? = null,
    @Json(name = "additionalProp1") val additionalProp1: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class HoldSlotResponse(
    @Json(name = "reservationId") val reservationId: String? = null,
    @Json(name = "shopId") val shopId: String? = null,
    @Json(name = "barberId") val barberId: String? = null,
    @Json(name = "barberName") val barberName: String? = null,
    @Json(name = "slotTime") val slotTime: String? = null,
    @Json(name = "slotDate") val slotDate: String? = null,
    @Json(name = "expiresAtTimestamp") val expiresAtTimestamp: Long? = null
)

@JsonClass(generateAdapter = true)
data class QuickQueuePreviewRequest(
    @Json(name = "shopId") val shopId: String,
    @Json(name = "barberId") val barberId: String,
    @Json(name = "serviceIds") val serviceIds: List<String>
)

@JsonClass(generateAdapter = true)
data class QuickQueuePreviewDto(
    @Json(name = "startTime") val startTime: String = "",
    @Json(name = "endTime") val endTime: String = "",
    @Json(name = "totalDurationMinutes") val totalDurationMinutes: Int = 30,
    @Json(name = "price") val price: Int = 0,
    @Json(name = "confirmationToken") val confirmationToken: String = "",
    @Json(name = "dateStr") val dateStr: String? = null,
    @Json(name = "barberId") val barberId: String? = null,
    @Json(name = "barberName") val barberName: String? = null,
    @Json(name = "shopName") val shopName: String? = null,
    @Json(name = "proposalCode") val proposalCode: String? = null
)

@JsonClass(generateAdapter = true)
data class QuickQueueConfirmRequest(
    @Json(name = "confirmationToken") val confirmationToken: String
)

@JsonClass(generateAdapter = true)
data class BarberAvailabilityResponseDto(
    @Json(name = "shopId") val shopId: String? = null,
    @Json(name = "barberId") val barberId: String? = null,
    @Json(name = "date") val date: String? = null,
    @Json(name = "timezone") val timezone: String? = null,
    @Json(name = "slotIntervalMinutes") val slotIntervalMinutes: Int? = 30,
    @Json(name = "totalDurationMinutes") val totalDurationMinutes: Int? = null,
    @Json(name = "slots") val slots: List<AvailabilitySlotDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AvailabilitySlotDto(
    @Json(name = "startTime") val startTime: String = "",
    @Json(name = "endTime") val endTime: String = "",
    @Json(name = "available") val available: Boolean = false,
    @Json(name = "status") val status: String? = null,
    @Json(name = "reasonCode") val reasonCode: String? = null
)

@JsonClass(generateAdapter = true)
data class QuickQueueConfirmDto(
    @Json(name = "reservationId") val reservationId: String = "",
    @Json(name = "startTime") val startTime: String? = null,
    @Json(name = "endTime") val endTime: String? = null,
    @Json(name = "dateStr") val dateStr: String? = null,
    @Json(name = "price") val price: Int? = null,
    @Json(name = "totalDurationMinutes") val totalDurationMinutes: Int? = null,
    @Json(name = "barberName") val barberName: String? = null,
    @Json(name = "shopId") val shopId: String? = null,
    @Json(name = "status") val status: String? = "HELD"
)

@JsonClass(generateAdapter = true)
data class CreateBookingRequest(
    @Json(name = "bookingNumber") val bookingNumber: String,
    @Json(name = "reservationId") val reservationId: String? = null,
    @Json(name = "userId") val userId: String? = null,
    @Json(name = "customerName") val customerName: String? = null,
    @Json(name = "customerPhone") val customerPhone: String? = null,
    @Json(name = "shopId") val shopId: String,
    @Json(name = "shopName") val shopName: String,
    @Json(name = "shopAddress") val shopAddress: String,
    @Json(name = "barberId") val barberId: String = "b1",
    @Json(name = "barberName") val barberName: String,
    @Json(name = "serviceNames") val serviceNames: List<String> = emptyList(),
    @Json(name = "serviceIds") val serviceIds: List<String> = emptyList(),
    @Json(name = "dateStr") val dateStr: String,
    @Json(name = "bookingDate") val bookingDate: String = "",
    @Json(name = "timeSlot") val timeSlot: String,
    @Json(name = "startTime") val startTime: String = "",
    @Json(name = "durationMinutes") val durationMinutes: Int = 30,
    @Json(name = "price") val price: Int,
    @Json(name = "tax") val tax: Int = 0,
    @Json(name = "total") val total: Int = price,
    @Json(name = "paymentMethod") val paymentMethod: String = "Pay at Salon (Post-service)",
    @Json(name = "bookingMode") val bookingMode: String? = "SCHEDULED"
)

@JsonClass(generateAdapter = true)
data class BookingDto(
    @Json(name = "bookingNumber") val bookingNumber: String? = null,
    @Json(name = "_id") val id: String? = null,
    @Json(name = "reservationId") val reservationId: String? = null,
    @Json(name = "userId") val userId: String? = null,
    @Json(name = "customerName") val customerName: String? = null,
    @Json(name = "customerPhone") val customerPhone: String? = null,
    @Json(name = "shopId") val shopId: String = "",
    @Json(name = "shopName") val shopName: String = "",
    @Json(name = "shopAddress") val shopAddress: String = "",
    @Json(name = "barberId") val barberId: String? = null,
    @Json(name = "barberName") val barberName: String = "",
    @Json(name = "serviceNames") val serviceNames: List<String> = emptyList(),
    @Json(name = "serviceIds") val serviceIds: List<String> = emptyList(),
    @Json(name = "dateStr") val dateStr: String = "",
    @Json(name = "bookingDate") val bookingDate: String? = null,
    @Json(name = "timeSlot") val timeSlot: String = "",
    @Json(name = "startTime") val startTime: String? = null,
    @Json(name = "durationMinutes") val durationMinutes: Int = 30,
    @Json(name = "price") val price: Int = 0,
    @Json(name = "tax") val tax: Int = 0,
    @Json(name = "total") val total: Int = 0,
    @Json(name = "status") val status: String = "UPCOMING",
    @Json(name = "paymentStatus") val paymentStatus: String = "PAID",
    @Json(name = "paymentMethod") val paymentMethod: String = "UPI",
    @Json(name = "isLate") val isLate: Boolean = false,
    @Json(name = "bookingMode") val bookingMode: String? = null
)

@JsonClass(generateAdapter = true)
data class RescheduleRequest(
    @Json(name = "newDateStr") val newDateStr: String,
    @Json(name = "newTimeSlot") val newTimeSlot: String,
    @Json(name = "newBarberName") val newBarberName: String? = null,
    @Json(name = "newDate") val newDate: String? = null,
    @Json(name = "newTime") val newTime: String? = null
)

@JsonClass(generateAdapter = true)
data class CancelBookingRequest(
    @Json(name = "reason") val reason: String = "Customer request"
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

// =========================================================================
// AUTH REQUEST & RESPONSE DTOs
// =========================================================================

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class UserLocationDto(
    @Json(name = "lat") val lat: Double = 28.6500,
    @Json(name = "lng") val lng: Double = 77.4500,
    @Json(name = "address") val address: String = "Noida, Uttar Pradesh"
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String,
    @Json(name = "phone") val phone: String,
    @Json(name = "password") val password: String,
    @Json(name = "location") val location: UserLocationDto? = null
)

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "_id") val id: String? = null,
    @Json(name = "id") val altId: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "avatar") val avatar: String? = null,
    @Json(name = "role") val role: String? = "customer"
)

@JsonClass(generateAdapter = true)
data class AuthDataDto(
    @Json(name = "user") val user: UserDto? = null,
    @Json(name = "token") val token: String? = null,
    @Json(name = "refreshToken") val refreshToken: String? = null
)

