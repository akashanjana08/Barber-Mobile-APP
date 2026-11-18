package com.example.data.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Business timezone and dynamic booking date validation for BarberCraft.
 * Enforces:
 * 1. Timezone: Asia/Kolkata
 * 2. Only upcoming 3 days (Today, Tomorrow, Day after tomorrow).
 * 3. Dynamic calculation, midnight transition handling, and strict validation.
 * 4. 100% compatible with all Android API versions (using java.util.Calendar / TimeZone).
 */
object BookingDateUtils {

    val BUSINESS_TIMEZONE: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    data class BookingDateItem(
        val fullDateStr: String,       // e.g. "29 September 2026"
        val shortDateStr: String,      // e.g. "29 Sep 2026"
        val dayNumber: String,         // e.g. "29"
        val monthName: String,         // e.g. "September"
        val shortMonthName: String,    // e.g. "Sep"
        val relativeLabel: String,     // "Today", "Tomorrow", "Day After"
        val year: Int = 2026,
        val month: Int = 9,
        val day: Int = 29
    )

    /**
     * Returns exactly the 3 allowed upcoming booking dates calculated dynamically
     * in the Asia/Kolkata timezone:
     * - Today
     * - Tomorrow
     * - Day after tomorrow
     */
    fun getAvailableBookingDates(timeZone: TimeZone = BUSINESS_TIMEZONE): List<BookingDateItem> {
        val cal = Calendar.getInstance(timeZone, Locale.ENGLISH).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val fullFormat = SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH).apply { this.timeZone = timeZone }
        val shortFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).apply { this.timeZone = timeZone }
        val monthNameFormat = SimpleDateFormat("MMMM", Locale.ENGLISH).apply { this.timeZone = timeZone }
        val shortMonthFormat = SimpleDateFormat("MMM", Locale.ENGLISH).apply { this.timeZone = timeZone }

        val relativeLabels = listOf("Today", "Tomorrow", "Day After")

        return (0..2).map { offset ->
            val loopCal = cal.clone() as Calendar
            loopCal.add(Calendar.DAY_OF_YEAR, offset)
            val date = loopCal.time

            val dayStr = String.format(Locale.ENGLISH, "%02d", loopCal.get(Calendar.DAY_OF_MONTH))
            val monthName = monthNameFormat.format(date)
            val shortMonth = shortMonthFormat.format(date)
            val fullStr = fullFormat.format(date)
            val shortStr = shortFormat.format(date)

            BookingDateItem(
                fullDateStr = fullStr,
                shortDateStr = shortStr,
                dayNumber = dayStr,
                monthName = monthName,
                shortMonthName = shortMonth,
                relativeLabel = relativeLabels.getOrElse(offset) { "Day +$offset" },
                year = loopCal.get(Calendar.YEAR),
                month = loopCal.get(Calendar.MONTH) + 1,
                day = loopCal.get(Calendar.DAY_OF_MONTH)
            )
        }
    }

    /**
     * Returns today's default booking date string in Asia/Kolkata.
     */
    fun getTodayFullDateStr(timeZone: TimeZone = BUSINESS_TIMEZONE): String {
        val cal = Calendar.getInstance(timeZone, Locale.ENGLISH)
        val format = SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH).apply { this.timeZone = timeZone }
        return format.format(cal.time)
    }

    /**
     * Validates whether a date string is within the allowed upcoming 3 days:
     * [today, today + 2 days] in Asia/Kolkata.
     * Rejects past dates and dates beyond 3 days.
     */
    fun isDateInAllowedWindow(dateStr: String?, timeZone: TimeZone = BUSINESS_TIMEZONE): Boolean {
        if (dateStr.isNullOrBlank()) return false
        val parsedCal = parseFlexibleDate(dateStr, timeZone) ?: return false

        val todayCal = Calendar.getInstance(timeZone, Locale.ENGLISH).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val maxCal = (todayCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 2)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        return !parsedCal.before(todayCal) && !parsedCal.after(maxCal)
    }

    /**
     * Parses arbitrary date string representations safely into a Calendar.
     * Supports "29 September 2026", "29 Sep 2026", "2026-09-29", "29/09/2026",
     * "Today", "Tomorrow", etc.
     */
    /**
     * Parses flexible date string into a Calendar instance at start-of-day.
     * Supports:
     * - "Today", "Tomorrow"
     * - "Today, 11 Oct 2026", "Tomorrow, 12 Oct 2026"
     * - "Sun, 11 Oct 2026", "Sunday, 11 October 2026"
     * - "11 October 2026", "11 Oct 2026", "11 Oct"
     * - ISO strings: "2026-10-11", "2026-10-11T10:30:00.000Z", "2026-10-11 10:30:00"
     * - Common formats: "dd/MM/yyyy", "dd-MM-yyyy", "yyyy/MM/dd", "MM/dd/yyyy", etc.
     */
    fun parseFlexibleDate(rawStr: String, timeZone: TimeZone = BUSINESS_TIMEZONE): Calendar? {
        val todayCal = Calendar.getInstance(timeZone, Locale.ENGLISH).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        var clean = rawStr.trim()
            .replace("(?i)\\(Today\\)|\\(Tomorrow\\)".toRegex(), "")
            .trim()

        if (clean.contains("T")) {
            clean = clean.substringBefore("T").trim()
        } else if (clean.contains(" ") && clean.matches("^\\d{4}-\\d{2}-\\d{2}\\s+.*".toRegex())) {
            clean = clean.substringBefore(" ").trim()
        }

        if (clean.startsWith("Today,", ignoreCase = true)) {
            clean = clean.substring(6).trim()
        } else if (clean.startsWith("Tomorrow,", ignoreCase = true)) {
            clean = clean.substring(9).trim()
        } else if (clean.equals("Today", ignoreCase = true)) {
            return todayCal
        } else if (clean.equals("Tomorrow", ignoreCase = true)) {
            val tomorrow = todayCal.clone() as Calendar
            tomorrow.add(Calendar.DAY_OF_YEAR, 1)
            return tomorrow
        }

        // Remove prefix day names like "Sun, " or "Sunday, "
        val cleanWithoutDayName = clean.replace("^[A-Za-z]+,\\s*".toRegex(), "").trim()

        val patterns = listOf(
            "EEE, dd MMM yyyy",
            "EEE, d MMM yyyy",
            "EEEE, dd MMMM yyyy",
            "EEEE, d MMMM yyyy",
            "EEE, dd MMMM yyyy",
            "EEE, d MMMM yyyy",
            "dd MMMM yyyy",
            "dd MMM yyyy",
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "MM/dd/yyyy",
            "d MMMM yyyy",
            "d MMM yyyy",
            "d/M/yyyy",
            "d-M-yyyy",
            "MMMM dd, yyyy",
            "MMM dd, yyyy",
            "MMMM d, yyyy",
            "MMM d, yyyy"
        )

        for (pattern in patterns) {
            for (candidate in listOf(clean, cleanWithoutDayName)) {
                try {
                    val sdf = SimpleDateFormat(pattern, Locale.ENGLISH).apply {
                        this.timeZone = timeZone
                        isLenient = false
                    }
                    val date = sdf.parse(candidate)
                    if (date != null) {
                        val cal = Calendar.getInstance(timeZone, Locale.ENGLISH).apply {
                            time = date
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        return cal
                    }
                } catch (_: Exception) {}
            }
        }

        // Try format without year: "d MMM" -> use current year
        try {
            val sdf = SimpleDateFormat("d MMM", Locale.ENGLISH).apply {
                this.timeZone = timeZone
                isLenient = false
            }
            val date = sdf.parse(cleanWithoutDayName)
            if (date != null) {
                val tempCal = Calendar.getInstance(timeZone, Locale.ENGLISH).apply { time = date }
                val cal = todayCal.clone() as Calendar
                cal.set(Calendar.MONTH, tempCal.get(Calendar.MONTH))
                cal.set(Calendar.DAY_OF_MONTH, tempCal.get(Calendar.DAY_OF_MONTH))
                return cal
            }
        } catch (_: Exception) {}

        return null
    }

    /**
     * Formats any raw date string into a clean, human-readable user-friendly format:
     * - "Today, 11 Oct 2026"
     * - "Tomorrow, 12 Oct 2026"
     * - "Sun, 11 Oct 2026"
     * Handles ISO timestamps, "yyyy-MM-dd", "dd MMMM yyyy", "dd MMM yyyy", etc.
     */
    fun formatUserFriendlyDate(rawDateStr: String?, includeYear: Boolean = true, timeZone: TimeZone = BUSINESS_TIMEZONE): String {
        val todayCal = Calendar.getInstance(timeZone, Locale.ENGLISH).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val datePattern = if (includeYear) "d MMM yyyy" else "d MMM"
        val df = SimpleDateFormat(datePattern, Locale.ENGLISH).apply { this.timeZone = timeZone }

        if (rawDateStr.isNullOrBlank()) {
            return "Today, ${df.format(todayCal.time)}"
        }

        val cal = parseFlexibleDate(rawDateStr, timeZone) ?: return rawDateStr

        val tomorrowCal = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }

        val isToday = isSameDay(cal, todayCal)
        val isTomorrow = isSameDay(cal, tomorrowCal)

        val formattedDate = df.format(cal.time)

        return when {
            isToday -> "Today, $formattedDate"
            isTomorrow -> "Tomorrow, $formattedDate"
            else -> {
                val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.ENGLISH).apply { this.timeZone = timeZone }
                "${dayOfWeekFormat.format(cal.time)}, $formattedDate"
            }
        }
    }

    /**
     * Formats 24-hour ("14:30", "14:30:00", "09:00", "9:00") or 12-hour strings into clean 12-hour AM/PM format:
     * e.g. "2:30 PM", "9:00 AM", "10:30 AM"
     * Also handles ISO datetimes like "2026-10-11T10:30:00.000Z" -> "10:30 AM"
     * And ranges like "10:30 - 11:15" -> "10:30 AM – 11:15 AM"
     */
    fun formatUserFriendlyTime(rawTimeStr: String?): String {
        if (rawTimeStr.isNullOrBlank()) return ""
        var clean = rawTimeStr.trim()

        // Strip ISO date portion if present
        if (clean.contains("T")) {
            clean = clean.substringAfter("T").substringBefore("Z").substringBefore("+").trim()
        } else if (clean.contains(" ") && clean.matches("^\\d{4}-\\d{2}-\\d{2}\\s+.*".toRegex())) {
            clean = clean.substringAfter(" ").trim()
        }

        // Check if string contains an internal range: "10:30 - 11:15" or "10:30 AM - 11:15 AM"
        if (clean.contains(" – ")) {
            val parts = clean.split(" – ")
            if (parts.size == 2) {
                val t1 = formatSingleTime(parts[0])
                val t2 = formatSingleTime(parts[1])
                if (t1.isNotBlank() && t2.isNotBlank()) return "$t1 – $t2"
            }
        } else if (clean.contains(" - ")) {
            val parts = clean.split(" - ")
            if (parts.size == 2) {
                val t1 = formatSingleTime(parts[0])
                val t2 = formatSingleTime(parts[1])
                if (t1.isNotBlank() && t2.isNotBlank()) return "$t1 – $t2"
            }
        }

        return formatSingleTime(clean)
    }

    private fun formatSingleTime(raw: String): String {
        val clean = raw.trim()
        if (clean.isBlank()) return ""

        val amPmMatch = "(?i)\\b(AM|PM)\\b".toRegex().find(clean)
        val explicitAmPm = amPmMatch?.value?.uppercase(Locale.ENGLISH)

        // Strip AM/PM and trailing time zone info for numerical hour/min parsing
        val withoutAmPm = clean.replace("(?i)\\s*(AM|PM)".toRegex(), "").trim()
        val parts = withoutAmPm.split(":")
        if (parts.size >= 2) {
            val hour = parts[0].toIntOrNull()
            val minute = parts[1].toIntOrNull()
            if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
                val finalAmPm = explicitAmPm ?: if (hour >= 12) "PM" else "AM"
                val displayHour = when {
                    explicitAmPm != null && hour in 1..12 -> hour
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }
                return String.format(Locale.ENGLISH, "%d:%02d %s", displayHour, minute, finalAmPm)
            }
        }

        if (clean.endsWith("AM", ignoreCase = true) || clean.endsWith("PM", ignoreCase = true)) {
            return clean.uppercase(Locale.ENGLISH)
        }

        return clean
    }

    /**
     * Formats a time range, e.g. "10:30 AM – 11:15 AM"
     */
    fun formatUserFriendlyTimeRange(startTime: String?, endTime: String?): String {
        if (startTime.isNullOrBlank() && endTime.isNullOrBlank()) return ""

        val s = formatUserFriendlyTime(startTime)
        val e = formatUserFriendlyTime(endTime)

        // If startTime already contains a full formatted range
        if (s.contains(" – ")) {
            return s
        }

        return when {
            s.isNotBlank() && e.isNotBlank() -> "$s – $e"
            s.isNotBlank() -> s
            e.isNotBlank() -> e
            else -> ""
        }
    }

    /**
     * Derives expected end time given a start time and duration in minutes.
     */
    fun deriveExpectedEndTime(startTimeStr: String?, durationMinutes: Int): String {
        if (startTimeStr.isNullOrBlank()) return ""
        val friendly = formatUserFriendlyTime(startTimeStr)
        val single = if (friendly.contains(" – ")) friendly.substringBefore(" – ") else friendly
        return try {
            val parts = single.trim().split(" ")
            val timeParts = parts[0].split(":")
            var hour = timeParts[0].toInt()
            val min = timeParts[1].toInt()
            val amPm = if (parts.size > 1) parts[1].uppercase(Locale.ENGLISH) else "AM"

            if (amPm == "PM" && hour < 12) hour += 12
            if (amPm == "AM" && hour == 12) hour = 0

            val totalMins = (hour * 60 + min + durationMinutes) % (24 * 60)
            val endHour24 = totalMins / 60
            val endMin = totalMins % 60
            val endAmPm = if (endHour24 >= 12) "PM" else "AM"
            val endHour12 = if (endHour24 == 0) 12 else if (endHour24 > 12) endHour24 - 12 else endHour24
            String.format(Locale.ENGLISH, "%d:%02d %s", endHour12, endMin, endAmPm)
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Formats combined date and time for booking summaries, e.g.:
     * "Today, 11 Oct 2026 • 2:30 PM" or "Sun, 11 Oct 2026 • 10:00 AM"
     */
    fun formatUserFriendlyDateTime(rawDateStr: String?, rawTimeStr: String?): String {
        val datePart = formatUserFriendlyDate(rawDateStr)
        val timePart = formatUserFriendlyTime(rawTimeStr)
        return when {
            datePart.isNotBlank() && timePart.isNotBlank() -> "$datePart • $timePart"
            datePart.isNotBlank() -> datePart
            timePart.isNotBlank() -> timePart
            else -> ""
        }
    }

    /**
     * Compact format for badges and list cards:
     * "Today • 2:30 PM" or "11 Oct • 2:30 PM"
     */
    fun formatCompactDateTime(rawDateStr: String?, rawTimeStr: String?): String {
        val datePart = formatUserFriendlyDate(rawDateStr, includeYear = false)
        val timePart = formatUserFriendlyTime(rawTimeStr)
        return when {
            datePart.isNotBlank() && timePart.isNotBlank() -> "$datePart • $timePart"
            datePart.isNotBlank() -> datePart
            timePart.isNotBlank() -> timePart
            else -> ""
        }
    }

    /**
     * Converts any date string to standard ISO "yyyy-MM-dd" format for API queries.
     * e.g. "11 October 2026" -> "2026-10-11"
     */
    fun toIsoDateString(rawDate: String?, timeZone: TimeZone = BUSINESS_TIMEZONE): String {
        val cal = if (!rawDate.isNullOrBlank()) parseFlexibleDate(rawDate, timeZone) else null
        val targetCal = cal ?: Calendar.getInstance(timeZone, Locale.ENGLISH)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply { this.timeZone = timeZone }
        return sdf.format(targetCal.time)
    }

    /**
     * Checks if a raw date string resolves to today in business timezone.
     */
    fun isToday(rawDateStr: String?, timeZone: TimeZone = BUSINESS_TIMEZONE): Boolean {
        if (rawDateStr.isNullOrBlank()) return true
        val clean = rawDateStr.trim()
        if (clean.equals("Today", ignoreCase = true) || clean.startsWith("Today,", ignoreCase = true)) return true
        if (clean.equals("Tomorrow", ignoreCase = true) || clean.startsWith("Tomorrow,", ignoreCase = true)) return false
        val cal = parseFlexibleDate(clean, timeZone) ?: return false
        val todayCal = Calendar.getInstance(timeZone, Locale.ENGLISH).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return isSameDay(cal, todayCal)
    }

    /**
     * Determines whether a given booking date and time slot has already passed
     * relative to the current moment in the specified timezone (Asia/Kolkata).
     *
     * Rules:
     * - If date is strictly before today: true (Past)
     * - If date is strictly after today: false (Upcoming)
     * - If date is today: compares slot's start/end time minutes to current time minutes
     */
    fun isSlotInPast(
        rawDateStr: String?,
        rawTimeStr: String?,
        timeZone: TimeZone = BUSINESS_TIMEZONE,
        referenceCal: Calendar = Calendar.getInstance(timeZone, Locale.ENGLISH)
    ): Boolean {
        val todayCal = (referenceCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val bookingCal = if (!rawDateStr.isNullOrBlank()) {
            parseFlexibleDate(rawDateStr, timeZone) ?: todayCal
        } else {
            todayCal
        }

        // 1. Strictly in the past day
        if (bookingCal.before(todayCal)) {
            return true
        }

        // 2. Strictly in a future day
        if (bookingCal.after(todayCal)) {
            return false
        }

        // 3. Same day (Today): Check time slot
        if (rawTimeStr.isNullOrBlank()) {
            return false
        }

        val slotMins = parseSlotTimeToMinutesOfDay(rawTimeStr)
        val currentMins = referenceCal.get(Calendar.HOUR_OF_DAY) * 60 + referenceCal.get(Calendar.MINUTE)

        return slotMins < currentMins
    }

    /**
     * Determines whether a given booking date and time slot is upcoming (now or future).
     */
    fun isSlotUpcoming(
        rawDateStr: String?,
        rawTimeStr: String?,
        timeZone: TimeZone = BUSINESS_TIMEZONE,
        referenceCal: Calendar = Calendar.getInstance(timeZone, Locale.ENGLISH)
    ): Boolean {
        return !isSlotInPast(rawDateStr, rawTimeStr, timeZone, referenceCal)
    }

    /**
     * Parses time strings into minutes from midnight (0..1439).
     * Supports "10:00 AM", "5:30 PM", "14:30", "10:30 – 11:15", "08:00", etc.
     */
    fun parseSlotTimeToMinutesOfDay(rawTimeStr: String?): Int {
        if (rawTimeStr.isNullOrBlank()) return 0
        var clean = rawTimeStr.trim()
        if (clean.contains("T")) {
            clean = clean.substringAfter("T").substringBefore("Z").substringBefore("+").trim()
        } else if (clean.contains(" ") && clean.matches("^\\d{4}-\\d{2}-\\d{2}\\s+.*".toRegex())) {
            clean = clean.substringAfter(" ").trim()
        }

        val singleTime = if (clean.contains(" – ")) {
            clean.substringBefore(" – ")
        } else if (clean.contains(" - ")) {
            clean.substringBefore(" - ")
        } else {
            clean
        }.trim()

        val upper = singleTime.uppercase(Locale.ENGLISH)
        val isPm = upper.contains("PM")
        val isAm = upper.contains("AM")
        val digitsOnly = upper.replace("AM", "").replace("PM", "").trim()
        val parts = digitsOnly.split(":")
        if (parts.isEmpty()) return 0

        var hours = parts[0].toIntOrNull() ?: 0
        val minutes = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0

        if (isPm && hours < 12) hours += 12
        if (isAm && hours == 12) hours = 0

        return (hours * 60 + minutes).coerceIn(0, 1439)
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
