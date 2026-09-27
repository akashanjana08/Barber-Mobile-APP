package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BookingEntity::class,
        FavoriteEntity::class,
        NotificationEntity::class,
        UserProfileEntity::class,
        RecoveredSlotOfferEntity::class,
        OfferDeliveryRecordEntity::class,
        ReminderRecordEntity::class,
        BarberQueueEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookingDao(): BookingDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun notificationDao(): NotificationDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun offerDao(): OfferDao
    abstract fun offerDeliveryRecordDao(): OfferDeliveryRecordDao
    abstract fun reminderRecordDao(): ReminderRecordDao
    abstract fun barberQueueDao(): BarberQueueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "barbercraft_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
