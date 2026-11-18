package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.util.BookingDateUtils
import com.example.ui.BarberViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    currentFilter: FilterCriteria,
    onDismiss: () -> Unit,
    onApply: (FilterCriteria) -> Unit
) {
    var distance by remember { mutableFloatStateOf(currentFilter.maxDistanceKm.toFloat()) }
    var minRating by remember { mutableFloatStateOf(currentFilter.minRating.toFloat()) }
    var maxPrice by remember { mutableFloatStateOf(currentFilter.maxPrice.toFloat()) }
    var openNowOnly by remember { mutableStateOf(currentFilter.openNowOnly) }
    var availableTodayOnly by remember { mutableStateOf(currentFilter.availableTodayOnly) }
    var selectedSort by remember { mutableStateOf(currentFilter.sortBy) }
    var selectedCategories by remember { mutableStateOf(currentFilter.selectedCategories) }

    val categories = listOf("Haircut", "Beard", "Hair Color", "Facial", "Hair Spa", "Kids Haircut")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Filter & Sort",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                TextButton(
                    onClick = {
                        distance = 10f
                        minRating = 0f
                        maxPrice = 1500f
                        openNowOnly = false
                        availableTodayOnly = false
                        selectedSort = SortOption.RECOMMENDED
                        selectedCategories = emptySet()
                    }
                ) {
                    Text("Reset All", color = AmberGold, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sort Section
            Text(
                text = "Sort By",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    SortOption.RECOMMENDED to "Recommended",
                    SortOption.NEAREST to "Nearest",
                    SortOption.HIGHEST_RATED to "Top Rated",
                    SortOption.LOWEST_PRICE to "Lowest ₹"
                ).forEach { (opt, label) ->
                    val isSelected = selectedSort == opt
                    Surface(
                        onClick = { selectedSort = opt },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) AmberGold else SurfaceElevated,
                        border = BorderStroke(1.dp, if (isSelected) AmberGoldLight else BorderSubtle),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) ObsidianDark else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Distance slider
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Max Distance", color = TextSecondary, fontSize = 14.sp)
                Text("${String.format("%.1f", distance)} km", color = AmberGold, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = distance,
                onValueChange = { distance = it },
                valueRange = 1f..10f,
                steps = 8,
                colors = SliderDefaults.colors(
                    thumbColor = AmberGold,
                    activeTrackColor = AmberGold,
                    inactiveTrackColor = SurfaceElevated
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Price slider
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Max Starting Price", color = TextSecondary, fontSize = 14.sp)
                Text("₹${maxPrice.toInt()}", color = AmberGold, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = maxPrice,
                onValueChange = { maxPrice = it },
                valueRange = 100f..1500f,
                steps = 13,
                colors = SliderDefaults.colors(
                    thumbColor = AmberGold,
                    activeTrackColor = AmberGold,
                    inactiveTrackColor = SurfaceElevated
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Rating pills
            Text("Minimum Rating", color = TextSecondary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.0 to "All", 4.0 to "4.0+", 4.5 to "4.5+", 4.8 to "4.8+").forEach { (r, label) ->
                    val isSelected = (minRating == r.toFloat())
                    Surface(
                        onClick = { minRating = r.toFloat() },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) AmberGold else SurfaceElevated,
                        border = BorderStroke(1.dp, if (isSelected) AmberGoldLight else BorderSubtle),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) ObsidianDark else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Categories
            Text("Services", color = TextSecondary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                categories.forEach { cat ->
                    val isChecked = selectedCategories.contains(cat)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedCategories = if (isChecked) {
                                    selectedCategories - cat
                                } else {
                                    selectedCategories + cat
                                }
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { check ->
                                selectedCategories = if (check) selectedCategories + cat else selectedCategories - cat
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = AmberGold,
                                checkmarkColor = ObsidianDark
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(cat, color = TextPrimary, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Open Now & Today Switches
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Now Only", color = TextPrimary, fontSize = 14.sp)
                Switch(
                    checked = openNowOnly,
                    onCheckedChange = { openNowOnly = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AmberGold,
                        checkedTrackColor = AmberGoldDark
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Apply Button
            Button(
                onClick = {
                    onApply(
                        FilterCriteria(
                            maxDistanceKm = distance.toDouble(),
                            minRating = minRating.toDouble(),
                            maxPrice = maxPrice.toInt(),
                            selectedCategories = selectedCategories,
                            openNowOnly = openNowOnly,
                            availableTodayOnly = availableTodayOnly,
                            sortBy = selectedSort
                        )
                    )
                    onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberGold,
                    contentColor = ObsidianDark
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("apply_filters_button")
            ) {
                Text("Apply Filters", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun LocationPickerModal(
    currentCity: String,
    onSelectCity: (String) -> Unit,
    onDetectGps: () -> Unit,
    isDetecting: Boolean,
    onDismiss: () -> Unit
) {
    val popularLocations = listOf(
        "Noida, Sector 18, Uttar Pradesh",
        "Noida, Sector 142, Uttar Pradesh",
        "South Delhi, Greater Kailash, Delhi",
        "Connaught Place, Central Delhi",
        "Cyber Hub, Gurugram, Haryana",
        "Indiranagar, Bengaluru, Karnataka",
        "Bandra West, Mumbai, Maharashtra"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                "Choose Your Location",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // GPS auto-detect button
                Surface(
                    onClick = onDetectGps,
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        if (isDetecting) {
                            CircularProgressIndicator(
                                color = AmberGold,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Detect",
                                tint = AmberGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Auto-Detect My Location",
                                color = AmberGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                "Using GPS coordinates & reverse geocoding",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Or select a popular area:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))

                popularLocations.forEach { loc ->
                    val isSelected = currentCity.contains(loc.take(15))
                    Surface(
                        onClick = {
                            onSelectCity(loc)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) SurfaceCard else Color.Transparent,
                        border = if (isSelected) BorderStroke(1.dp, AmberGold) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = if (isSelected) AmberGold else TextTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = loc,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = AmberGold)
            }
        }
    )
}

@Composable
fun CancelBookingDialog(
    booking: Booking,
    onConfirmCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Cancel",
                tint = CrimsonRed,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                "Cancel Appointment?",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to cancel your appointment on ${BookingDateUtils.formatUserFriendlyDateTime(booking.dateStr, booking.timeSlot)} with ${booking.barberName} at ${booking.shopName}?",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Cancellation Policy",
                            fontWeight = FontWeight.Bold,
                            color = AmberGold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Free cancellation up to 2 hours before start.\n• 100% refund of ₹${booking.total} will be initiated back to original payment method.\n• Slot will be made available for other clients.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmCancel,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Yes, Cancel Booking", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Text("Keep Appointment", color = TextPrimary)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RescheduleDialog(
    booking: Booking,
    viewModel: BarberViewModel? = null,
    onConfirmReschedule: (newDateStr: String, newTimeSlot: String, newBarberName: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val availableDateItems = remember { BookingDateUtils.getAvailableBookingDates() }
    val availableBarbers = remember(booking) {
        listOf(
            booking.barberName,
            "Rahul Sharma",
            "Vikram Singh",
            "Amit Patel"
        ).filter { it.isNotBlank() }.distinct()
    }

    var selectedDate by remember { mutableStateOf(availableDateItems.first().fullDateStr) }
    var selectedTime by remember { mutableStateOf(booking.timeSlot.ifBlank { "10:00 AM" }) }
    var selectedBarber by remember { mutableStateOf(booking.barberName.ifBlank { availableBarbers.first() }) }
    var slotIntervalMinutes by remember { mutableIntStateOf(30) }

    var isLoadingSlots by remember { mutableStateOf(false) }
    var apiSlotsMap by remember { mutableStateOf<Map<String, List<Pair<String, Boolean>>>>(emptyMap()) }

    // Fetch availability from API whenever date, barber, or slotIntervalMinutes changes
    LaunchedEffect(selectedDate, selectedBarber, slotIntervalMinutes) {
        isLoadingSlots = true
        val targetShopId = booking.shopId.ifBlank { "shop_9606dbb5-74a5-4634-8eef-d2375ffb6ab9" }
        val targetBarberId = booking.barberId ?: "b1"
        val targetServiceIds = booking.serviceIds.ifEmpty { listOf("s1") }

        if (viewModel != null) {
            val fetched = viewModel.getRescheduleAvailabilitySlots(
                shopId = targetShopId,
                barberId = targetBarberId,
                date = selectedDate,
                serviceIds = targetServiceIds,
                slotIntervalMinutes = slotIntervalMinutes
            )
            apiSlotsMap = fetched
        } else {
            // Authoritative fallback matching API logic if viewModel not passed
            val morning = mutableListOf<Pair<String, Boolean>>()
            val afternoon = mutableListOf<Pair<String, Boolean>>()
            val evening = mutableListOf<Pair<String, Boolean>>()
            var currentMin = 8 * 60
            val endLimit = 22 * 60
            val isToday = BookingDateUtils.isToday(selectedDate)
            while (currentMin < endLimit) {
                val h = currentMin / 60
                val m = currentMin % 60
                val timeStr = String.format(java.util.Locale.ENGLISH, "%02d:%02d", h, m)
                val available = when {
                    currentMin >= 21 * 60 -> true
                    currentMin == 20 * 60 + 30 -> false
                    currentMin in (10 * 60)..(16 * 60) && (currentMin % 60 == 0) -> true
                    isToday && currentMin < 12 * 60 -> false
                    else -> currentMin % 90 == 0
                }
                val pair = timeStr to available
                when {
                    h < 12 -> morning.add(pair)
                    h in 12..15 -> afternoon.add(pair)
                    else -> evening.add(pair)
                }
                currentMin += slotIntervalMinutes
            }
            val map = mutableMapOf<String, List<Pair<String, Boolean>>>()
            if (morning.isNotEmpty()) map["Morning"] = morning
            if (afternoon.isNotEmpty()) map["Afternoon"] = afternoon
            if (evening.isNotEmpty()) map["Evening"] = evening
            apiSlotsMap = map
        }
        isLoadingSlots = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text("Reschedule Appointment", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Current: ${BookingDateUtils.formatUserFriendlyDateTime(booking.dateStr, booking.timeSlot)} with ${booking.barberName}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: Dynamic 3-Day Date Selection with Current Date highlight (like Schedule Screen)
                Text(
                    "Select Date",
                    color = AmberGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    availableDateItems.forEach { dateItem ->
                        val isSelected = selectedDate == dateItem.fullDateStr || selectedDate == dateItem.shortDateStr
                        val isCurrentDate = dateItem.relativeLabel.equals("Today", ignoreCase = true) || BookingDateUtils.isToday(dateItem.fullDateStr)
                        Surface(
                            onClick = { selectedDate = dateItem.fullDateStr },
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isSelected -> AmberGold
                                isCurrentDate -> AmberGold.copy(alpha = 0.16f)
                                else -> SurfaceCard
                            },
                            border = BorderStroke(
                                if (isCurrentDate || isSelected) 2.dp else 1.dp,
                                when {
                                    isSelected -> AmberGoldLight
                                    isCurrentDate -> AmberGold
                                    else -> BorderSubtle
                                }
                            ),
                            modifier = Modifier
                                .width(108.dp)
                                .testTag("reschedule_date_${dateItem.dayNumber}")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            ) {
                                if (isCurrentDate) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isSelected) ObsidianDark else AmberGold,
                                        modifier = Modifier.padding(bottom = 3.dp)
                                    ) {
                                        Text(
                                            text = "CURRENT DATE",
                                            color = if (isSelected) AmberGold else ObsidianDark,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 7.5.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${dateItem.dayNumber} ${dateItem.shortMonthName}",
                                    color = if (isSelected) ObsidianDark else TextPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isCurrentDate) "Today" else dateItem.relativeLabel,
                                    color = if (isSelected) ObsidianDark.copy(alpha = 0.85f) else if (isCurrentDate) AmberGold else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Slot Interval Selector (default 30m)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("reschedule_slot_interval_selector")
                ) {
                    Text("Slot Interval:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    listOf(15, 30, 45, 60).forEach { interval ->
                        val isSelectedInterval = slotIntervalMinutes == interval
                        Surface(
                            onClick = { slotIntervalMinutes = interval },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelectedInterval) AmberGold else SurfaceCard,
                            border = BorderStroke(1.dp, if (isSelectedInterval) AmberGold else BorderSubtle),
                            modifier = Modifier.testTag("reschedule_interval_${interval}m")
                        ) {
                            Text(
                                text = "${interval}m",
                                color = if (isSelectedInterval) ObsidianDark else TextPrimary,
                                fontWeight = if (isSelectedInterval) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Date Availability Pill indicator (Like Schedule Screen)
                val isViewingCurrentDate = BookingDateUtils.isToday(selectedDate)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isViewingCurrentDate) AmberGold.copy(alpha = 0.14f) else SurfaceCard,
                    border = BorderStroke(1.dp, if (isViewingCurrentDate) AmberGold.copy(alpha = 0.6f) else BorderSubtle),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(if (isViewingCurrentDate) AmberGold else TextSecondary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Availability: ${BookingDateUtils.formatUserFriendlyDate(selectedDate)}",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )
                        }
                        if (isViewingCurrentDate) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AmberGold
                            ) {
                                Text(
                                    text = "TODAY",
                                    color = ObsidianDark,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 8.5.sp,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // API Slot list grouped by Morning/Afternoon/Evening like Schedule Screen
                Text(
                    "Select Time Slot",
                    color = AmberGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (isLoadingSlots) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth().padding(24.dp)
                    ) {
                        CircularProgressIndicator(color = AmberGold, modifier = Modifier.size(28.dp))
                    }
                } else if (apiSlotsMap.isEmpty()) {
                    Text(
                        "No time slots available from API for selected parameters.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    apiSlotsMap.forEach { (timeOfDay, slots) ->
                        Text(
                            text = "$timeOfDay Slots",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            slots.forEach { (slotTime, isAvailable) ->
                                val friendlyTime = BookingDateUtils.formatUserFriendlyTime(slotTime)
                                val isSelected = selectedTime == slotTime || selectedTime == friendlyTime
                                Surface(
                                    onClick = {
                                        if (isAvailable) {
                                            selectedTime = friendlyTime
                                        } else {
                                            Toast.makeText(context, "Slot is unavailable. Please pick an open time.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = when {
                                        !isAvailable -> SurfaceDark.copy(alpha = 0.5f)
                                        isSelected -> AmberGold
                                        else -> SurfaceCard
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        when {
                                            !isAvailable -> BorderSubtle.copy(alpha = 0.4f)
                                            isSelected -> AmberGoldLight
                                            else -> BorderSubtle
                                        }
                                    ),
                                    modifier = Modifier.width(90.dp).testTag("reschedule_slot_${slotTime.replace(":", "_").replace(" ", "_")}")
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = friendlyTime,
                                            color = when {
                                                !isAvailable -> TextTertiary
                                                isSelected -> ObsidianDark
                                                else -> TextPrimary
                                            },
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = if (isAvailable) "Available" else "Unavailable",
                                            color = when {
                                                !isAvailable -> CrimsonRed.copy(alpha = 0.7f)
                                                isSelected -> ObsidianDark
                                                else -> EmeraldGreen
                                            },
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Barber selection
                Text("Select Master Barber", color = AmberGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    availableBarbers.take(3).forEach { barber ->
                        val isSelected = selectedBarber == barber
                        Surface(
                            onClick = { selectedBarber = barber },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) AmberGold else SurfaceElevated,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = barber.split(" ").first(),
                                color = if (isSelected) ObsidianDark else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmReschedule(selectedDate, selectedTime, selectedBarber) },
                colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianDark),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Confirm Reschedule", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun ReviewDialog(
    booking: Booking,
    onSubmitReview: (rating: Int, comment: String) -> Unit,
    onDismiss: () -> Unit
) {
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    val tags = listOf("Punctual", "Clean Studio", "Great Fade", "Polite Barber", "Worth the Price")
    val selectedTags = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text("Rate Your Experience", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${booking.shopName} • ${booking.barberName}",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Stars
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { rating = star }) {
                            Icon(
                                imageVector = if (star <= rating) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Star $star",
                                tint = if (star <= rating) StarYellow else TextTertiary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick tags
                Text("What did you like?", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tags.take(3).forEach { tag ->
                        val isSelected = selectedTags.contains(tag)
                        Surface(
                            onClick = {
                                if (isSelected) selectedTags.remove(tag) else selectedTags.add(tag)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) AmberGold else SurfaceElevated,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = tag,
                                color = if (isSelected) ObsidianDark else TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    placeholder = { Text("Write your review...", color = TextTertiary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberGold,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmitReview(rating, comment) },
                colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianDark),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Submit Review", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun DirectionsDialog(
    shop: BarberShop,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        icon = {
            Icon(Icons.Default.Directions, contentDescription = null, tint = AmberGold, modifier = Modifier.size(36.dp))
        },
        title = {
            Text("Directions to ${shop.name}", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text("📍 ${shop.address}", color = TextPrimary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Distance: ${shop.distanceKm} km from your current location.", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceElevated,
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🗺️ Map Navigation Ready", color = AmberGold, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(shop.name + " " + shop.address)}")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    try {
                        context.startActivity(mapIntent)
                    } catch (e: Exception) {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(shop.name + " " + shop.address)}"))
                        context.startActivity(browserIntent)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianDark),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Open in Maps", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        }
    )
}

@Composable
fun BookingDetailDialog(
    booking: Booking,
    onDismiss: () -> Unit,
    onReschedule: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Appointment Details",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (booking.bookingMode.contains("QUICK", ignoreCase = true)) EmeraldGreenBg else AmberGlow
                ) {
                    Text(
                        text = if (booking.bookingMode.contains("QUICK", ignoreCase = true)) "⚡ Quick Queue" else "📅 Scheduled",
                        color = if (booking.bookingMode.contains("QUICK", ignoreCase = true)) EmeraldGreen else AmberGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Booking Reference", color = TextSecondary, fontSize = 12.sp)
                            Text(booking.id, fontWeight = FontWeight.Bold, color = AmberGold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Salon", color = TextSecondary, fontSize = 12.sp)
                            Text(booking.shopName, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Stylist", color = TextSecondary, fontSize = 12.sp)
                            Text(booking.barberName, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Date & Time", color = TextSecondary, fontSize = 12.sp)
                            Text(BookingDateUtils.formatUserFriendlyDateTime(booking.dateStr, booking.timeSlot), fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Services Booked:", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                booking.serviceNames.forEach { serviceName ->
                    Text("• $serviceName", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = BorderSubtle)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Payment Status", color = TextSecondary, fontSize = 13.sp)
                    Text("${booking.paymentStatus.name} (${booking.paymentMethod})", color = EmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Total Price", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                    Text("₹${booking.total}", fontWeight = FontWeight.ExtraBold, color = AmberGold, fontSize = 16.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianDark),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            if (booking.status == BookingStatus.UPCOMING && onCancel != null) {
                OutlinedButton(
                    onClick = onCancel,
                    border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel Booking", color = CrimsonRed, fontSize = 12.sp)
                }
            }
        }
    )
}

