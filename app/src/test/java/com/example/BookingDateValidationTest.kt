package com.example

import com.example.data.util.BookingDateUtils
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class BookingDateValidationTest {

    @Test
    fun testOnlyUpcoming3DaysReturned() {
        val dates = BookingDateUtils.getAvailableBookingDates()
        assertEquals(3, dates.size)
        assertEquals("Today", dates[0].relativeLabel)
        assertEquals("Tomorrow", dates[1].relativeLabel)
        assertEquals("Day After", dates[2].relativeLabel)

        val todayCal = Calendar.getInstance(BookingDateUtils.BUSINESS_TIMEZONE, Locale.ENGLISH)
        assertEquals(todayCal.get(Calendar.DAY_OF_MONTH), dates[0].day)
    }

    @Test
    fun testAllowedWindowValidation() {
        val todayStr = BookingDateUtils.getTodayFullDateStr()
        val dates = BookingDateUtils.getAvailableBookingDates()

        // Valid dates
        assertTrue(BookingDateUtils.isDateInAllowedWindow(todayStr))
        assertTrue(BookingDateUtils.isDateInAllowedWindow(dates[0].fullDateStr))
        assertTrue(BookingDateUtils.isDateInAllowedWindow(dates[1].fullDateStr))
        assertTrue(BookingDateUtils.isDateInAllowedWindow(dates[2].fullDateStr))

        // Past date must NOT be allowed
        assertFalse(BookingDateUtils.isDateInAllowedWindow("01 January 2020"))
        assertFalse(BookingDateUtils.isDateInAllowedWindow("2020-01-01"))

        // Far future date must NOT be allowed
        assertFalse(BookingDateUtils.isDateInAllowedWindow("01 January 2030"))
        assertFalse(BookingDateUtils.isDateInAllowedWindow("2030-01-01"))
    }

    @Test
    fun testFlexibleDateParsing() {
        val parsed = BookingDateUtils.parseFlexibleDate("29 September 2026")
        assertNotNull(parsed)
        assertEquals(2026, parsed?.get(Calendar.YEAR))
        assertEquals(Calendar.SEPTEMBER, parsed?.get(Calendar.MONTH))
        assertEquals(29, parsed?.get(Calendar.DAY_OF_MONTH))

        val parsedShort = BookingDateUtils.parseFlexibleDate("30 Sep 2026")
        assertNotNull(parsedShort)
        assertEquals(2026, parsedShort?.get(Calendar.YEAR))
        assertEquals(Calendar.SEPTEMBER, parsedShort?.get(Calendar.MONTH))
        assertEquals(30, parsedShort?.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testUserFriendlyDateFormatting() {
        val todayStr = BookingDateUtils.getTodayFullDateStr()
        val friendlyToday = BookingDateUtils.formatUserFriendlyDate(todayStr)
        assertTrue(friendlyToday.startsWith("Today,"))

        val dates = BookingDateUtils.getAvailableBookingDates()
        val friendlyTomorrow = BookingDateUtils.formatUserFriendlyDate(dates[1].fullDateStr)
        assertTrue(friendlyTomorrow.startsWith("Tomorrow,"))

        // ISO format date parsing and formatting
        val isoDate = "2026-10-15T12:00:00.000Z"
        val friendlyIso = BookingDateUtils.formatUserFriendlyDate(isoDate)
        assertTrue(friendlyIso.contains("15 Oct 2026"))
    }

    @Test
    fun testUserFriendlyTimeFormatting() {
        assertEquals("2:30 PM", BookingDateUtils.formatUserFriendlyTime("14:30"))
        assertEquals("2:30 PM", BookingDateUtils.formatUserFriendlyTime("14:30:00"))
        assertEquals("9:00 AM", BookingDateUtils.formatUserFriendlyTime("09:00"))
        assertEquals("9:00 AM", BookingDateUtils.formatUserFriendlyTime("9:00"))
        assertEquals("12:00 PM", BookingDateUtils.formatUserFriendlyTime("12:00"))
        assertEquals("12:00 AM", BookingDateUtils.formatUserFriendlyTime("00:00"))
        assertEquals("10:30 AM", BookingDateUtils.formatUserFriendlyTime("10:30 AM"))
        assertEquals("10:30 AM", BookingDateUtils.formatUserFriendlyTime("10:30"))
        assertEquals("10:30 AM", BookingDateUtils.formatUserFriendlyTime("10:30:00 AM"))
        assertEquals("10:30 AM", BookingDateUtils.formatUserFriendlyTime("2026-10-11T10:30:00.000Z"))
        assertEquals("10:30 AM – 11:15 AM", BookingDateUtils.formatUserFriendlyTime("10:30 - 11:15"))
    }

    @Test
    fun testUserFriendlyTimeRangeFormatting() {
        assertEquals("10:30 AM – 11:15 AM", BookingDateUtils.formatUserFriendlyTimeRange("10:30 AM", "11:15 AM"))
        assertEquals("10:30 AM – 11:15 AM", BookingDateUtils.formatUserFriendlyTimeRange("10:30", "11:15"))
        assertEquals("2:30 PM – 3:15 PM", BookingDateUtils.formatUserFriendlyTimeRange("14:30", "15:15"))
        assertEquals("10:30 AM – 11:15 AM", BookingDateUtils.formatUserFriendlyTimeRange("10:30 - 11:15", ""))
        assertEquals("10:30 AM", BookingDateUtils.formatUserFriendlyTimeRange("10:30 AM", null))
    }

    @Test
    fun testDeriveExpectedEndTime() {
        assertEquals("11:15 AM", BookingDateUtils.deriveExpectedEndTime("10:30 AM", 45))
        assertEquals("3:15 PM", BookingDateUtils.deriveExpectedEndTime("14:30", 45))
        assertEquals("11:15 AM", BookingDateUtils.deriveExpectedEndTime("10:30 - 11:00", 45))
    }

    @Test
    fun testUserFriendlyDateTimeCombined() {
        val todayStr = BookingDateUtils.getTodayFullDateStr()
        val formatted = BookingDateUtils.formatUserFriendlyDateTime(todayStr, "14:30")
        assertTrue(formatted.startsWith("Today,"))
        assertTrue(formatted.endsWith("2:30 PM"))
    }

    @Test
    fun testToIsoDateString() {
        val iso = BookingDateUtils.toIsoDateString("11 October 2026")
        assertEquals("2026-10-11", iso)
        assertEquals("2026-10-12", BookingDateUtils.toIsoDateString("12 October 2026"))
        assertEquals("2026-10-13", BookingDateUtils.toIsoDateString("13 October 2026"))
        assertEquals("2026-10-11", BookingDateUtils.toIsoDateString("2026-10-11"))
        assertEquals("2026-10-13", BookingDateUtils.toIsoDateString("2026-10-13"))
    }

    @Test
    fun testIsToday() {
        val todayStr = BookingDateUtils.getTodayFullDateStr()
        assertTrue(BookingDateUtils.isToday(todayStr))
        assertTrue(BookingDateUtils.isToday("Today"))
        assertFalse(BookingDateUtils.isToday("01 January 2020"))
    }

    @Test
    fun testHoldSlotAndBookingRequestPayloadCompleteness() {
        val holdReq = com.example.data.network.HoldSlotRequest(
            shopId = "shop_123",
            barberId = "b1",
            barberName = "Rahul Sharma",
            startTime = "10:00 AM",
            bookingDate = "2026-10-12",
            slotTime = "5:30 PM",
            slotDate = "2026-09-28",
            durationMinutes = 45,
            serviceIds = listOf("s1"),
            serviceNames = listOf("Signature Royal Haircut"),
            simulateConflict = true
        )
        assertEquals("b1", holdReq.barberId)
        assertEquals("shop_123", holdReq.shopId)
        assertEquals("5:30 PM", holdReq.slotTime)
        assertEquals("10:00 AM", holdReq.startTime)
        assertEquals("2026-10-12", holdReq.bookingDate)
        assertEquals(45, holdReq.durationMinutes)
        assertTrue(holdReq.simulateConflict)

        val createReq = com.example.data.network.CreateBookingRequest(
            bookingNumber = "BK12345",
            reservationId = "RESEEFF66",
            shopId = "shop_123",
            shopName = "BarberCraft",
            shopAddress = "123 Main St",
            barberId = "b1",
            barberName = "Rahul Sharma",
            serviceNames = listOf("Signature Royal Haircut"),
            serviceIds = listOf("s1"),
            dateStr = "12 October 2026",
            bookingDate = "2026-10-12",
            timeSlot = "08:00",
            startTime = "08:00",
            durationMinutes = 30,
            price = 300,
            tax = 15,
            total = 315,
            paymentMethod = "Pay at Salon (Post-service)"
        )
        assertNotNull(createReq.barberId)
        assertEquals("b1", createReq.barberId)
        assertTrue(createReq.serviceIds.isNotEmpty())
        assertEquals("s1", createReq.serviceIds.first())
        assertEquals("2026-10-12", createReq.bookingDate)
        assertEquals("08:00", createReq.startTime)
    }

    @Test
    fun testUpcomingSlotFiltering() {
        // Create a fixed reference Calendar: 2026-10-12 at 14:00 (2:00 PM)
        val fixedCal = Calendar.getInstance(BookingDateUtils.BUSINESS_TIMEZONE, Locale.ENGLISH).apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 12)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 1. Past dates should be past
        assertTrue(BookingDateUtils.isSlotInPast("11 October 2026", "10:00 AM", referenceCal = fixedCal))
        assertFalse(BookingDateUtils.isSlotUpcoming("11 October 2026", "10:00 AM", referenceCal = fixedCal))

        // 2. Future dates should be upcoming regardless of time slot
        assertFalse(BookingDateUtils.isSlotInPast("13 October 2026", "08:00 AM", referenceCal = fixedCal))
        assertTrue(BookingDateUtils.isSlotUpcoming("13 October 2026", "08:00 AM", referenceCal = fixedCal))

        // 3. Same day (2026-10-12) earlier time slots should be past
        assertTrue(BookingDateUtils.isSlotInPast("12 October 2026", "08:00", referenceCal = fixedCal))
        assertTrue(BookingDateUtils.isSlotInPast("12 October 2026", "10:00 AM", referenceCal = fixedCal))
        assertTrue(BookingDateUtils.isSlotInPast("12 October 2026", "1:30 PM", referenceCal = fixedCal))
        assertFalse(BookingDateUtils.isSlotUpcoming("12 October 2026", "1:30 PM", referenceCal = fixedCal))

        // 4. Same day (2026-10-12) later time slots should be upcoming
        assertFalse(BookingDateUtils.isSlotInPast("12 October 2026", "2:30 PM", referenceCal = fixedCal))
        assertTrue(BookingDateUtils.isSlotUpcoming("12 October 2026", "2:30 PM", referenceCal = fixedCal))
        assertTrue(BookingDateUtils.isSlotUpcoming("12 October 2026", "16:00", referenceCal = fixedCal))
        assertTrue(BookingDateUtils.isSlotUpcoming("12 October 2026", "5:30 PM", referenceCal = fixedCal))
    }
}
