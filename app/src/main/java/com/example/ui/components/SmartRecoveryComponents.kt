package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.BarberViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun FlashOfferBanner(
    offer: SlotOffer,
    onBookNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    var remainingSeconds by remember(offer.expiresAt) {
        val diff = (offer.expiresAt - System.currentTimeMillis()) / 1000
        mutableLongStateOf(diff.coerceAtLeast(0))
    }

    LaunchedEffect(offer.expiresAt) {
        while (remainingSeconds > 0) {
            delay(1000)
            val diff = (offer.expiresAt - System.currentTimeMillis()) / 1000
            remainingSeconds = diff.coerceAtLeast(0)
        }
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceElevated
        ),
        border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(CrimsonRed, AmberGold))),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onBookNow() }
            .testTag("flash_offer_banner_${offer.id}")
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            CrimsonRed.copy(alpha = 0.15f),
                            SurfaceCard
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = CrimsonRed.copy(alpha = 0.2f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = CrimsonRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LAST-MINUTE RECOVERED SLOT",
                        fontWeight = FontWeight.Black,
                        color = CrimsonRed,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }

                // Countdown badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ObsidianDark,
                    border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = AmberGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (remainingSeconds > 0) "Expires in $timeFormatted" else "Expired",
                            color = if (remainingSeconds > 0) AmberGold else CrimsonRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${offer.shopName} • ⭐ ${offer.shopRating}",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "${offer.serviceName} with ${offer.barberName}",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Today ${offer.timeSlot}",
                            color = AmberGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• 📍 ${offer.distanceKm} km away",
                            color = TextTertiary,
                            fontSize = 12.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "₹${offer.originalPrice}",
                            color = TextTertiary,
                            fontSize = 13.sp,
                            textDecoration = TextDecoration.LineThrough
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "₹${offer.offerPrice}",
                            color = EmeraldGreen,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                    Text(
                        text = "${offer.discountPercent}% OFF",
                        color = CrimsonRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onBookNow,
                enabled = remainingSeconds > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberGold,
                    contentColor = ObsidianDark
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("book_flash_offer_btn_${offer.id}")
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Book Recovered Slot (₹${offer.offerPrice})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun FlashOfferModal(
    offer: SlotOffer,
    isBooking: Boolean,
    error: String?,
    success: Booking?,
    simulateConflict: Boolean,
    onToggleConflict: () -> Unit,
    onConfirmBooking: () -> Unit,
    onDismiss: () -> Unit
) {
    var remainingSeconds by remember(offer.expiresAt) {
        val diff = (offer.expiresAt - System.currentTimeMillis()) / 1000
        mutableLongStateOf(diff.coerceAtLeast(0))
    }

    LaunchedEffect(offer.expiresAt) {
        while (remainingSeconds > 0) {
            delay(1000)
            val diff = (offer.expiresAt - System.currentTimeMillis()) / 1000
            remainingSeconds = diff.coerceAtLeast(0)
        }
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(22.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = CrimsonRed,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Last-Minute Barber Offer",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = offer.shopName,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "📍 ${offer.shopAddress} (${offer.distanceKm} km away)",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = BorderSubtle)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text("Service", color = TextTertiary, fontSize = 11.sp)
                                Text(offer.serviceName, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Barber", color = TextTertiary, fontSize = 11.sp)
                                Text(offer.barberName, color = AmberGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text("Time Slot", color = TextTertiary, fontSize = 11.sp)
                                Text("${offer.dateStr} • ${offer.timeSlot}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Original", color = TextTertiary, fontSize = 11.sp)
                                Text("₹${offer.originalPrice}", color = TextTertiary, textDecoration = TextDecoration.LineThrough, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = EmeraldGreenBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text("Special Discounted Total:", color = EmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("₹${offer.offerPrice} (20% OFF)", color = EmeraldGreen, fontWeight = FontWeight.Black, fontSize = 15.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Timer Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (remainingSeconds > 0) AmberGlow else CrimsonRedBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = if (remainingSeconds > 0) AmberGold else CrimsonRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (remainingSeconds > 0) "Offer Expires In: $timeFormatted" else "This offer has expired",
                            color = if (remainingSeconds > 0) AmberGold else CrimsonRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Concurrency Simulation Toggle
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Simulate Concurrency Race (409)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Tests atomic locking when another customer claims slot", color = TextTertiary, fontSize = 10.sp)
                        }
                        Switch(
                            checked = simulateConflict,
                            onCheckedChange = { onToggleConflict() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CrimsonRed,
                                checkedTrackColor = CrimsonRed.copy(alpha = 0.4f)
                            )
                        )
                    }
                }

                // Error or Success Feedback
                if (error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CrimsonRedBg,
                        border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                color = CrimsonRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (success != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EmeraldGreenBg,
                        border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Booking Confirmed! Appointment ID: ${success.id}",
                                color = EmeraldGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (success == null) {
                Button(
                    onClick = onConfirmBooking,
                    enabled = !isBooking && remainingSeconds > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberGold,
                        contentColor = ObsidianDark
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_claim_offer_btn")
                ) {
                    if (isBooking) {
                        CircularProgressIndicator(color = ObsidianDark, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Confirm & Book Now", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (success == null) "Cancel" else "Close", color = TextSecondary)
            }
        }
    )
}

@Composable
fun AppointmentReminderModal(
    booking: Booking,
    onGetDirections: () -> Unit,
    onViewBooking: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(22.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = AmberGold,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Appointment Reminder",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Your appointment at ${booking.shopName} starts in 15 minutes.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = booking.shopName,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "📍 1.2 km away • ${booking.shopAddress}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = BorderSubtle)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text("Service", color = TextTertiary, fontSize = 11.sp)
                                Text(booking.serviceNames.joinToString(", "), color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Barber", color = TextTertiary, fontSize = 11.sp)
                                Text(booking.barberName, color = AmberGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text("Scheduled Time", color = TextTertiary, fontSize = 11.sp)
                                Text("${booking.timeSlot} • ${booking.dateStr}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Status", color = TextTertiary, fontSize = 11.sp)
                                Text(booking.status.name, color = EmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Real-time queue waiting count note
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = AmberGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Real-Time Queue: 2 customers ahead",
                                color = AmberGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${booking.barberName} is on schedule (~30 mins estimated wait).",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Arrival note
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AmberGlow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Please reach the barber shop before your scheduled time.",
                            color = AmberGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // If customer is already late (Section 55 rule)
                if (booking.isLate) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CrimsonRedBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Your appointment started at ${booking.timeSlot}. Please contact the barber shop if you are running late.",
                                color = CrimsonRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onGetDirections,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberGold,
                    contentColor = ObsidianDark
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("reminder_get_directions_btn")
            ) {
                Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Get Directions", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onViewBooking) {
                Text("View Booking", color = AmberGold)
            }
        }
    )
}

@Composable
fun SmartRecoveryEngineConsoleModal(
    viewModel: BarberViewModel,
    onDismiss: () -> Unit
) {
    val candidates by viewModel.targetCandidates.collectAsState()
    val isSimulatingReminder by viewModel.isSimulatingReminder.collectAsState()
    val isSimulatingRecovery by viewModel.isSimulatingRecovery.collectAsState()
    val simulateConflict by viewModel.simulateOfferConflict.collectAsState()
    val permissionEnabled by viewModel.locationPermissionGranted.collectAsState()
    val upcomingBookings by viewModel.upcomingBookings.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(22.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = AmberGold,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = ObsidianDark, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Smart Recovery & Scheduling Engine", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Backend architecture & targeting simulation", color = TextSecondary, fontSize = 11.sp)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Architectural Summary Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "ARCHITECTURE & STATE MACHINE",
                            color = AmberGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Confirmed Booking ➔ 15-Min Arrival Reminder ➔ On Cancellation/No-Show ➔ Release Slot ➔ Geospatial (<=2km) & Activity (<=48h) Filtering ➔ 20% Flash Discount ➔ Anti-Spam Deduplication ➔ Atomic Lock (409 Conflict Prevention)",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Engine Triggers
                Text("Simulate Backend Events", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                // Trigger 1: 15-Min Reminder
                OutlinedButton(
                    onClick = {
                        val firstUpcoming = upcomingBookings.firstOrNull()?.id ?: "BK10234"
                        viewModel.trigger15MinuteReminder(firstUpcoming)
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSimulatingReminder) {
                        CircularProgressIndicator(color = AmberGold, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Alarm, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Trigger 15-Minute Arrival Reminder", color = AmberGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Trigger 2: Cancellation Recovery
                OutlinedButton(
                    onClick = {
                        val firstUpcoming = upcomingBookings.firstOrNull()?.id ?: "BK10234"
                        viewModel.triggerCancellationRecovery(firstUpcoming)
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSimulatingRecovery) {
                        CircularProgressIndicator(color = CrimsonRed, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Cancel, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simulate Customer Cancellation & Recovery", color = CrimsonRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Trigger 3: Barber Marks No-Show
                OutlinedButton(
                    onClick = {
                        val firstUpcoming = upcomingBookings.firstOrNull()?.id ?: "BK10234"
                        viewModel.markCustomerNoShow(firstUpcoming)
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PersonOff, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simulate Barber Marking 'NO_SHOW'", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Privacy / Location Permission Control (Section 61)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("User Location Permission", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = if (permissionEnabled) "Granted • Device can receive nearby offers" else "Denied • Marketing tracking disabled",
                                color = if (permissionEnabled) EmeraldGreen else CrimsonRed,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = permissionEnabled,
                            onCheckedChange = { viewModel.toggleLocationPermission() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AmberGold,
                                checkedTrackColor = AmberGold.copy(alpha = 0.4f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Targeting Candidate Evaluation Table (Section 60)
                Text("Targeting Evaluation (Royal Barber Shop: 28.6500, 77.4500)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    candidates.forEach { c ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceCard,
                            border = BorderStroke(1.dp, if (c.isEligible) EmeraldGreen.copy(alpha = 0.4f) else BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(c.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("Dist: ${c.distanceKm} km • Active: ${c.lastActiveHoursAgo.toInt()}h ago", color = TextSecondary, fontSize = 11.sp)
                                    Text(c.ineligibilityReason, color = if (c.isEligible) EmeraldGreen else TextTertiary, fontSize = 10.sp)
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (c.isEligible) EmeraldGreenBg else CrimsonRedBg
                                ) {
                                    Text(
                                        text = if (c.isEligible) "ELIGIBLE" else "INELIGIBLE",
                                        color = if (c.isEligible) EmeraldGreen else CrimsonRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianDark),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Close Console", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun NoShowConfirmDialog(
    booking: Booking,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Confirm Customer No-Show", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to mark ${booking.barberName}'s appointment with booking ID ${booking.id} as a No-Show?",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Once marked as NO_SHOW, the remaining slot time will immediately enter the smart recovery pipeline to be offered to nearby customers at a flash discount.",
                        color = AmberGold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Mark as No-Show", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
