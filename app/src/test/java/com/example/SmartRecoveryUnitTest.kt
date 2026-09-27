package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.local.AppDatabase
import com.example.data.local.BookingEntity
import com.example.data.model.BookingStatus
import com.example.data.model.OfferSourceType
import com.example.data.model.PaymentStatus
import com.example.data.repository.BarberRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmartRecoveryUnitTest {

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
    fun `test 15-minute arrival reminder dispatch and deduplication`() = runBlocking {
        // Given an upcoming booking
        val booking = BookingEntity(
            id = "BK_TEST_REM",
            shopId = "shop_1",
            shopName = "Royal Barber Club & Lounge",
            shopAddress = "Sector 18, Noida",
            serviceNamesJoined = "Signature Royal Haircut",
            barberName = "Rahul Sharma",
            dateStr = "25 Sep 2026",
            timeSlot = "5:30 PM",
            price = 300,
            tax = 15,
            total = 315,
            status = BookingStatus.UPCOMING.name,
            paymentStatus = PaymentStatus.PAID.name,
            paymentMethod = "UPI",
            createdAt = System.currentTimeMillis()
        )
        database.bookingDao().insertBooking(booking)

        // When reminder check runs
        val reminded = repository.checkAndSendAppointmentReminders(forceForBookingId = "BK_TEST_REM")
        assertEquals(1, reminded.size)
        assertEquals("BK_TEST_REM", reminded.first().id)

        // Then reminder record is saved and second check does not send duplicate
        val secondRun = repository.checkAndSendAppointmentReminders()
        assertTrue(secondRun.isEmpty())

        // Verify reminder notification was generated
        val notifs = repository.getNotificationsFlow().first()
        assertTrue(notifs.any { it.title.contains("Appointment Reminder") })
    }

    @Test
    fun `test candidate targeting filter adheres to 2km and 48hr rules`() {
        val evaluated = repository.evaluateTargetCustomers(shopLat = 28.6500, shopLng = 77.4500)

        // Customer A: 0.8 km, 3h -> Eligible
        val custA = evaluated.find { it.id == "usr_A" }
        assertNotNull(custA)
        assertTrue(custA!!.isEligible)

        // Customer C: 2.8 km -> Ineligible (Distance > 2 km)
        val custC = evaluated.find { it.id == "usr_C" }
        assertNotNull(custC)
        assertFalse(custC!!.isEligible)
        assertTrue(custC.ineligibilityReason.contains("Distance > 2 km"))

        // Customer D: 1.2 km, 120h (5 days) -> Ineligible (Inactive > 2 days)
        val custD = evaluated.find { it.id == "usr_D" }
        assertNotNull(custD)
        assertFalse(custD!!.isEligible)
        assertTrue(custD.ineligibilityReason.contains("Inactive > 2 days"))
    }

    @Test
    fun `test cancelled slot recovery generates 20 percent discount offer`() = runBlocking {
        val booking = BookingEntity(
            id = "BK_CANCEL_REC",
            shopId = "shop_1",
            shopName = "Royal Barber Club & Lounge",
            shopAddress = "Sector 18, Noida",
            serviceNamesJoined = "Signature Royal Haircut",
            barberName = "Rahul Sharma",
            dateStr = "25 Sep 2026",
            timeSlot = "5:30 PM",
            price = 300,
            tax = 15,
            total = 315,
            status = BookingStatus.UPCOMING.name,
            paymentStatus = PaymentStatus.PAID.name,
            paymentMethod = "UPI",
            createdAt = System.currentTimeMillis()
        )
        database.bookingDao().insertBooking(booking)

        // When booking is cancelled
        repository.cancelBooking("BK_CANCEL_REC")

        // Then active offer is created with 20% discount (300 -> 240)
        val activeOffers = repository.getActiveOffersFlow().first()
        assertTrue(activeOffers.isNotEmpty())
        val offer = activeOffers.first()
        assertEquals(300, offer.originalPrice)
        assertEquals(240, offer.offerPrice)
        assertEquals(20, offer.discountPercent)
        assertEquals("Rahul Sharma", offer.barberName)
    }

    @Test
    fun `test atomic reservation prevents multiple bookings and handles 409 conflict`() = runBlocking {
        val booking = BookingEntity(
            id = "BK_CONFLICT_REC",
            shopId = "shop_1",
            shopName = "Royal Barber Club & Lounge",
            shopAddress = "Sector 18, Noida",
            serviceNamesJoined = "Signature Royal Haircut",
            barberName = "Rahul Sharma",
            dateStr = "25 Sep 2026",
            timeSlot = "5:30 PM",
            price = 300,
            tax = 15,
            total = 315,
            status = BookingStatus.UPCOMING.name,
            paymentStatus = PaymentStatus.PAID.name,
            paymentMethod = "UPI",
            createdAt = System.currentTimeMillis()
        )
        database.bookingDao().insertBooking(booking)
        val offerRes = repository.triggerSlotRecovery("BK_CONFLICT_REC", OfferSourceType.CUSTOMER_CANCELLATION)
        val offer = offerRes.getOrThrow()

        // When conflict is simulated (simulating another customer claiming it milliseconds prior)
        val failResult = repository.claimSlotOffer(offer.id, userId = "usr_102", simulateConflict = true)
        assertTrue(failResult.isFailure)
        assertTrue(failResult.exceptionOrNull()?.message?.contains("409 Conflict") == true)

        // When customer legitimately claims it
        val successResult = repository.claimSlotOffer(offer.id, userId = "usr_101", simulateConflict = false)
        assertTrue(successResult.isSuccess)
        val confirmedBooking = successResult.getOrThrow()
        assertEquals(offer.offerPrice, confirmedBooking.price)

        // Subsequent attempt to claim the same slot fails because it is already booked
        val secondAttempt = repository.claimSlotOffer(offer.id, userId = "usr_103", simulateConflict = false)
        assertTrue(secondAttempt.isFailure)
    }
}
