package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings ORDER BY createdAt DESC")
    fun getAllBookings(): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE id = :id LIMIT 1")
    suspend fun getBookingById(id: String): BookingEntity?

    @Query("SELECT * FROM bookings WHERE status = 'UPCOMING'")
    suspend fun getUpcomingBookingsList(): List<BookingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: BookingEntity)

    @Query("UPDATE bookings SET status = :status WHERE id = :id")
    suspend fun updateBookingStatus(id: String, status: String)

    @Query("UPDATE bookings SET reminderSentAt = :sentAt WHERE id = :id")
    suspend fun updateReminderSentAt(id: String, sentAt: Long)

    @Query("UPDATE bookings SET dateStr = :newDate, timeSlot = :newTime, status = 'UPCOMING' WHERE id = :id")
    suspend fun rescheduleBooking(id: String, newDate: String, newTime: String)

    @Query("DELETE FROM bookings WHERE id = :id")
    suspend fun deleteBooking(id: String)
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE shopId = :shopId")
    suspend fun removeFavorite(shopId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE shopId = :shopId)")
    suspend fun isFavorite(shopId: String): Boolean
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfileEntity)
}

@Dao
interface OfferDao {
    @Query("SELECT * FROM recovered_slot_offers ORDER BY createdAt DESC")
    fun getAllOffers(): Flow<List<RecoveredSlotOfferEntity>>

    @Query("SELECT * FROM recovered_slot_offers WHERE status IN ('CREATED', 'SENT', 'VIEWED') ORDER BY createdAt DESC")
    fun getActiveOffers(): Flow<List<RecoveredSlotOfferEntity>>

    @Query("SELECT * FROM recovered_slot_offers WHERE id = :id LIMIT 1")
    suspend fun getOfferById(id: String): RecoveredSlotOfferEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffer(offer: RecoveredSlotOfferEntity)

    @Query("UPDATE recovered_slot_offers SET status = :status WHERE id = :id")
    suspend fun updateOfferStatus(id: String, status: String)

    @Query("UPDATE recovered_slot_offers SET status = :status, reservedByUserId = :userId WHERE id = :id")
    suspend fun reserveOffer(id: String, userId: String, status: String)
}

@Dao
interface OfferDeliveryRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: OfferDeliveryRecordEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM offer_delivery_records WHERE userId = :userId AND shopId = :shopId AND timeSlot = :timeSlot AND dateStr = :dateStr)")
    suspend fun hasBeenDelivered(userId: String, shopId: String, timeSlot: String, dateStr: String): Boolean
}

@Dao
interface ReminderRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ReminderRecordEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM reminder_records WHERE bookingId = :bookingId)")
    suspend fun hasReminderBeenSent(bookingId: String): Boolean
}

@Dao
interface BarberQueueDao {
    @Query("SELECT * FROM barber_queue ORDER BY scheduledTimeMinutes ASC")
    fun getAllQueueFlow(): Flow<List<BarberQueueEntity>>

    @Query("SELECT * FROM barber_queue WHERE barberName = :barberName ORDER BY scheduledTimeMinutes ASC")
    fun getQueueByBarberFlow(barberName: String): Flow<List<BarberQueueEntity>>

    @Query("SELECT * FROM barber_queue ORDER BY scheduledTimeMinutes ASC")
    suspend fun getAllQueueList(): List<BarberQueueEntity>

    @Query("SELECT * FROM barber_queue WHERE barberName = :barberName ORDER BY scheduledTimeMinutes ASC")
    suspend fun getQueueByBarberList(barberName: String): List<BarberQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItems(items: List<BarberQueueEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItem(item: BarberQueueEntity)

    @Query("UPDATE barber_queue SET status = :status, lastUpdated = :now WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE barber_queue SET status = :status, remainingMinutes = :remaining, lastUpdated = :now WHERE id = :id")
    suspend fun updateStatusAndRemaining(id: String, status: String, remaining: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE barber_queue SET scheduledTime = :newTime, scheduledTimeMinutes = :newMinutes, lastUpdated = :now WHERE id = :id")
    suspend fun updateScheduledTime(id: String, newTime: String, newMinutes: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE barber_queue SET delayMinutes = delayMinutes + :delayMins, remainingMinutes = remainingMinutes + :delayMins, lastUpdated = :now WHERE barberName = :barberName AND status IN ('CONFIRMED', 'CHECKED_IN', 'IN_PROGRESS', 'DELAYED')")
    suspend fun addBarberDelay(barberName: String, delayMins: Int, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM barber_queue WHERE id = :id")
    suspend fun deleteQueueItem(id: String)

    @Query("DELETE FROM barber_queue")
    suspend fun clearQueue()
}
