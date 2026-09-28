package com.example.data.network

import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Health
    @GET("health")
    suspend fun getHealth(): Response<HealthResponse>

    // Shops
    @GET("shops")
    suspend fun getShops(
        @Query("search") search: String? = null,
        @Query("category") category: String? = null,
        @Query("sortBy") sortBy: String? = null
    ): Response<ApiResponse<List<ShopDto>>>

    @GET("shops/{id}")
    suspend fun getShopById(@Path("id") shopId: String): Response<ApiResponse<ShopDto>>

    @POST("shops/{id}/favorite")
    suspend fun toggleShopFavorite(@Path("id") shopId: String): Response<ApiResponse<Map<String, Any>>>

    // Bookings & Slot Lock
    @POST("bookings/hold")
    suspend fun holdSlot(@Body request: HoldSlotRequest): Response<ApiResponse<HoldSlotResponse>>

    @POST("bookings")
    suspend fun createBooking(@Body request: CreateBookingRequest): Response<ApiResponse<BookingDto>>

    @GET("bookings")
    suspend fun getBookings(): Response<ApiResponse<List<BookingDto>>>

    @POST("bookings/{id}/cancel")
    suspend fun cancelBooking(
        @Path("id") bookingId: String,
        @Body reason: Map<String, String> = emptyMap()
    ): Response<ApiResponse<BookingDto>>

    @POST("bookings/{id}/reschedule")
    suspend fun rescheduleBooking(
        @Path("id") bookingId: String,
        @Body request: RescheduleRequest
    ): Response<ApiResponse<BookingDto>>

    // Queue Endpoints
    @GET("queue/summaries")
    suspend fun getQueueSummaries(): Response<ApiResponse<Map<String, BarberSummaryDto>>>

    @GET("queue/position")
    suspend fun getQueuePosition(
        @Query("bookingId") bookingId: String,
        @Query("barberName") barberName: String,
        @Query("scheduledTime") scheduledTime: String
    ): Response<ApiResponse<QueuePositionDto>>

    @POST("queue/start")
    suspend fun startQueueService(@Body request: Map<String, String>): Response<ApiResponse<Any>>

    @POST("queue/complete")
    suspend fun completeQueueService(@Body request: Map<String, String>): Response<ApiResponse<Any>>

    @POST("queue/checkin")
    suspend fun checkInQueue(@Body request: Map<String, String>): Response<ApiResponse<Any>>

    @POST("queue/reset")
    suspend fun resetQueue(): Response<ApiResponse<Any>>

    // Smart Recovery Flash Offers
    @GET("offers/active")
    suspend fun getActiveOffers(): Response<ApiResponse<List<OfferDto>>>

    @POST("offers/{id}/claim")
    suspend fun claimOffer(
        @Path("id") offerId: String,
        @Body request: Map<String, String>
    ): Response<ApiResponse<ClaimOfferResponse>>

    // Notifications
    @GET("notifications")
    suspend fun getNotifications(): Response<ApiResponse<List<NotificationDto>>>

    @PUT("notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<ApiResponse<Any>>
}
