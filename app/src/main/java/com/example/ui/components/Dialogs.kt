package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
                    text = "Are you sure you want to cancel your appointment with ${booking.barberName} at ${booking.shopName}?",
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

@Composable
fun RescheduleDialog(
    booking: Booking,
    onConfirmReschedule: (newDate: String, newTime: String) -> Unit,
    onDismiss: () -> Unit
) {
    val dates = listOf("26 Sep 2026", "27 Sep 2026", "28 Sep 2026", "29 Sep 2026")
    val times = listOf("11:00 AM", "01:30 PM", "04:00 PM", "06:00 PM", "07:30 PM")

    var selectedDate by remember { mutableStateOf(dates.first()) }
    var selectedTime by remember { mutableStateOf(times.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text("Reschedule Appointment", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    "Current slot: ${booking.timeSlot} on ${booking.dateStr}",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text("Select New Date", color = AmberGold, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    dates.take(3).forEach { d ->
                        val isSelected = selectedDate == d
                        Surface(
                            onClick = { selectedDate = d },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) AmberGold else SurfaceElevated,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = d.take(6),
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

                Text("Select Available Time", color = AmberGold, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    times.chunked(3).forEach { rowTimes ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowTimes.forEach { t ->
                                val isSelected = selectedTime == t
                                Surface(
                                    onClick = { selectedTime = t },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) AmberGold else SurfaceElevated,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = t,
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
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmReschedule(selectedDate, selectedTime) },
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
