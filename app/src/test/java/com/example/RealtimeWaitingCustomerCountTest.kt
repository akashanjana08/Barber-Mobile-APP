package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.local.AppDatabase
import com.example.data.local.BarberQueueEntity
import com.example.data.model.QueueCustomerStatus
import com.example.data.repository.BarberRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RealtimeWaitingCustomerCountTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: BarberRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = BarberRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `test Rule 1 - Waiting count is barber-specific`() = runBlocking {
        // Given Rahul has 3 customers and Amit has 1 customer
        val queueDao = database.barberQueueDao()
        queueDao.insertQueueItems(
            listOf(
                BarberQueueEntity(
                    id = "Q_R1",
                    barberId = "b1",
                    barberName = "Rahul Sharma",
                    shopId = "shop_1",
                    customerName = "Customer A",
                    serviceNamesJoined = "Haircut",
                    durationMinutes = 30,
                    scheduledTime = "5:00 PM",
                    scheduledTimeMinutes = 1020,
                    status = QueueCustomerStatus.CONFIRMED.name,
                    remainingMinutes = 30
                ),
                BarberQueueEntity(
                    id = "Q_R2",
                    barberId = "b1",
                    barberName = "Rahul Sharma",
                    shopId = "shop_1",
                    customerName = "Customer B",
                    serviceNamesJoined = "Beard",
                    durationMinutes = 15,
                    scheduledTime = "5:15 PM",
                    scheduledTimeMinutes = 1035,
                    status = QueueCustomerStatus.CONFIRMED.name,
                    remainingMinutes = 15
                ),
                BarberQueueEntity(
                    id = "Q_R3",
                    barberId = "b1",
                    barberName = "Rahul Sharma",
                    shopId = "shop_1",
                    customerName = "Customer C",
                    serviceNamesJoined = "Haircut",
                    durationMinutes = 30,
                    scheduledTime = "5:45 PM",
                    scheduledTimeMinutes = 1065,
                    status = QueueCustomerStatus.CONFIRMED.name,
                    remainingMinutes = 30
                ),
                // Amit's customer
                BarberQueueEntity(
                    id = "Q_A1",
                    barberId = "b2",
                    barberName = "Amit Verma",
                    shopId = "shop_1",
                    customerName = "Customer D",
                    serviceNamesJoined = "Skin Fade",
                    durationMinutes = 15,
                    scheduledTime = "5:00 PM",
                    scheduledTimeMinutes = 1020,
                    status = QueueCustomerStatus.CONFIRMED.name,
                    remainingMinutes = 15
                )
            )
        )

        val summaries = repository.getBarberQueueSummariesFlow().first()

        // Rahul has 3 customers waiting
        assertEquals(3, summaries["Rahul Sharma"]?.activeWaitingCount)

        // Amit has 1 customer waiting
        assertEquals(1, summaries["Amit Verma"]?.activeWaitingCount)
    }

    @Test
    fun `test Rule 2 - Only relevant bookings are counted and cancelled-completed-noshow are excluded`() = runBlocking {
        val queueDao = database.barberQueueDao()
        queueDao.insertQueueItems(
            listOf(
                BarberQueueEntity(
                    id = "Q_1",
                    barberId = "b1",
                    barberName = "Rahul Sharma",
                    shopId = "shop_1",
                    customerName = "Customer A",
                    serviceNamesJoined = "Haircut",
                    durationMinutes = 30,
                    scheduledTime = "4:30 PM",
                    scheduledTimeMinutes = 990,
                    status = QueueCustomerStatus.COMPLETED.name, // EXCLUDED
                    remainingMinutes = 0
                ),
                BarberQueueEntity(
                    id = "Q_2",
                    barberId = "b1",
                    barberName = "Rahul Sharma",
                    shopId = "shop_1",
                    customerName = "Customer B",
                    serviceNamesJoined = "Beard",
                    durationMinutes = 15,
                    scheduledTime = "4:45 PM",
                    scheduledTimeMinutes = 1005,
                    status = QueueCustomerStatus.CANCELLED.name, // EXCLUDED
                    remainingMinutes = 15
                ),
                BarberQueueEntity(
                    id = "Q_3",
                    barberId = "b1",
                    barberName = "Rahul Sharma",
                    shopId = "shop_1",
                    customerName = "Customer C",
                    serviceNamesJoined = "Haircut",
                    durationMinutes = 30,
                    scheduledTime = "5:00 PM",
                    scheduledTimeMinutes = 1020,
                    status = QueueCustomerStatus.NO_SHOW.name, // EXCLUDED
                    remainingMinutes = 30
                ),
                BarberQueueEntity(
                    id = "Q_4",
                    barberId = "b1",
                    barberName = "Rahul Sharma",
                    shopId = "shop_1",
                    customerName = "Customer D",
                    serviceNamesJoined = "Haircut",
                    durationMinutes = 30,
                    scheduledTime = "5:15 PM",
                    scheduledTimeMinutes = 1035,
                    status = QueueCustomerStatus.IN_PROGRESS.name, // INCLUDED
                    remainingMinutes = 15
                ),
                BarberQueueEntity(
                    id = "Q_5",
                    barberId = "b1",
                    barberName = "Rahul Sharma",
                    shopId = "shop_1",
                    customerName = "Customer E",
                    serviceNamesJoined = "Beard",
                    durationMinutes = 15,
                    scheduledTime = "5:20 PM",
                    scheduledTimeMinutes = 1040,
                    status = QueueCustomerStatus.CHECKED_IN.name, // INCLUDED
                    remainingMinutes = 15
                )
            )
        )

        val position = repository.getUserQueuePositionFlow(
            bookingId = "MY_BOOKING",
            barberName = "Rahul Sharma",
            scheduledTime = "5:30 PM"
        ).first()

        // Only Q_4 and Q_5 are counted; Q_1 (COMPLETED), Q_2 (CANCELLED), Q_3 (NO_SHOW) are excluded!
        assertEquals(2, position.customersBeforeCount)
    }

    @Test
    fun `test Rule 3 - Count customers scheduled before current customer appointment`() = runBlocking {
        // Customer A: 5:00 PM, CONFIRMED
        // Customer B: 5:15 PM, CONFIRMED
        // Current Customer: 5:30 PM, CONFIRMED
        // Customer C: 5:45 PM, CONFIRMED
        val queueDao = database.barberQueueDao()
        queueDao.insertQueueItems(
            listOf(
                BarberQueueEntity(
                    id = "QA",
                    barberId = "b1",
                    barberName = "Rahul Sharma",
                    shopId = "shop_1",
                    customerName = "Customer A",
                    serviceNamesJoined = "Haircut",
                    durationMinutes = 30,
                    scheduledTime = "5:00 PM",
                    scheduledTimeMinutes = 1020,
                    status = QueueCustomerStatus.CONFIRMED.name,
                    remainingMinutes = 30
                ),
                BarberQueueEntity(
                    id = "QB",
                    barberId = "b1",
                    barberName = "Rahul Sharma",
                    shopId = "shop_1",
                    customerName = "Customer B",
                    serviceNamesJoined = "Beard",
                    durationMinutes = 15,
                    scheduledTime = "5:15 PM",
                    scheduledTimeMinutes = 1035,
                    status = QueueCustomerStatus.CONFIRMED.name,
                    remainingMinutes = 15
                ),
                BarberQueueEntity(
                    id = "QC_CURRENT",
                    barberId = "b1",
                    barberName = "Rahul Sharma",
                    shopId = "shop_1",
                    customerName = "Current Customer",
                    isCurrentUser = true,
                    bookingId = "BK_CURR_530",
                    serviceNamesJoined = "Haircut",
                    durationMinutes = 30,
                    scheduledTime = "5:30 PM",
                    scheduledTimeMinutes = 1050,
                    status = QueueCustomerStatus.CONFIRMED.name,
                    remainingMinutes = 30
                ),
                BarberQueueEntity(
                    id = "QD",
                    barberId = "b1",
                    barberName = "Rahul Sharma",
                    shopId = "shop_1",
                    customerName = "Customer C",
                    serviceNamesJoined = "Facial",
                    durationMinutes = 30,
                    scheduledTime = "5:45 PM",
                    scheduledTimeMinutes = 1065,
                    status = QueueCustomerStatus.CONFIRMED.name,
                    remainingMinutes = 30
                )
            )
        )

        val position = repository.getUserQueuePositionFlow(
            bookingId = "BK_CURR_530",
            barberName = "Rahul Sharma",
            scheduledTime = "5:30 PM"
        ).first()

        // Current customer must see exactly 2 customers before them (Customer A & B, NOT Customer C!)
        assertEquals(2, position.customersBeforeCount)
        assertEquals(listOf("Customer A", "Customer B"), position.customersAhead.map { it.customerName })
    }

    @Test
    fun `test Rule 4 - Service duration is considered for estimated wait time`() = runBlocking {
        // A -> 5:00-5:45 (45 min)
        // B -> 5:45-6:00 (15 min)
        // Current Customer -> 6:00 PM
        val queueDao = database.barberQueueDao()
        queueDao.insertQueueItems(
            listOf(
                BarberQueueEntity(
                    id = "Q_A_45",
                    barberId = "b1",
                    barberName = "Rahul Sharma",
                    shopId = "shop_1",
                    customerName = "Customer A",
                    serviceNamesJoined = "Haircut + Beard Combo",
                    durationMinutes = 45,
                    scheduledTime = "5:00 PM",
                    scheduledTimeMinutes = 1020,
                    status = QueueCustomerStatus.CONFIRMED.name,
                    remainingMinutes = 45
                ),
                BarberQueueEntity(
                    id = "Q_B_15",
                    barberId = "b1",
                    barberName = "Rahul Sharma",
                    shopId = "shop_1",
                    customerName = "Customer B",
                    serviceNamesJoined = "Beard Sculpt",
                    durationMinutes = 15,
                    scheduledTime = "5:45 PM",
                    scheduledTimeMinutes = 1065,
                    status = QueueCustomerStatus.CONFIRMED.name,
                    remainingMinutes = 15
                )
            )
        )

        val position = repository.getUserQueuePositionFlow(
            bookingId = "BK_CURR_600",
            barberName = "Rahul Sharma",
            scheduledTime = "6:00 PM"
        ).first()

        assertEquals(2, position.customersBeforeCount)
        // Estimated wait must be sum of service durations: 45 + 15 = 60 mins
        assertEquals(60, position.estimatedWaitingMinutes)
    }

    @Test
    fun `test Rule 5 - Near real-time updates when service completes, cancels, or delays`() = runBlocking {
        val queueDao = database.barberQueueDao()
        queueDao.insertQueueItems(
            listOf(
                BarberQueueEntity(
                    id = "Q_A_LIVE",
                    barberId = "b1",
                    barberName = "Rahul Sharma",
                    shopId = "shop_1",
                    customerName = "Customer A",
                    serviceNamesJoined = "Haircut",
                    durationMinutes = 30,
                    scheduledTime = "5:00 PM",
                    scheduledTimeMinutes = 1020,
                    status = QueueCustomerStatus.IN_PROGRESS.name,
                    remainingMinutes = 15
                ),
                BarberQueueEntity(
                    id = "Q_B_LIVE",
                    barberId = "b1",
                    barberName = "Rahul Sharma",
                    shopId = "shop_1",
                    customerName = "Customer B",
                    serviceNamesJoined = "Beard",
                    durationMinutes = 15,
                    scheduledTime = "5:15 PM",
                    scheduledTimeMinutes = 1035,
                    status = QueueCustomerStatus.CHECKED_IN.name,
                    remainingMinutes = 15
                )
            )
        )

        // 1. Initial State: 2 customers ahead, 30 mins wait
        val initialPos = repository.getUserQueuePositionFlow("BK_LIVE", "Rahul Sharma", "5:30 PM").first()
        assertEquals(2, initialPos.customersBeforeCount)
        assertEquals(30, initialPos.estimatedWaitingMinutes)

        // 2. Customer A completes service -> Count becomes 1 ahead, wait time 15 mins
        repository.completeBarberService("Q_A_LIVE")
        val afterComplete = repository.getUserQueuePositionFlow("BK_LIVE", "Rahul Sharma", "5:30 PM").first()
        assertEquals(1, afterComplete.customersBeforeCount)
        assertEquals(15, afterComplete.estimatedWaitingMinutes)

        // 3. Customer B cancels -> Count becomes 0 ahead, wait time 0 mins (Your turn!)
        repository.customerCancelQueueItem("Q_B_LIVE")
        val afterCancel = repository.getUserQueuePositionFlow("BK_LIVE", "Rahul Sharma", "5:30 PM").first()
        assertEquals(0, afterCancel.customersBeforeCount)
        assertEquals(0, afterCancel.estimatedWaitingMinutes)
    }
}
