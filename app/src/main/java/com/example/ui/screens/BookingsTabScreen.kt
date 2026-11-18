package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.util.BookingDateUtils
import com.example.ui.BarberViewModel
import com.example.ui.components.RealtimeQueueWaitingCard
import com.example.ui.theme.*

@Composable
fun BookingsTabScreen(
    viewModel: BarberViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedAppointmentTab.collectAsState()
    val rawAppointmentsList by viewModel.apiAppointments.collectAsState()
    val isLoading by viewModel.isLoadingAppointments.collectAsState()
    val errorMsg by viewModel.appointmentError.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    // Strictly filter out past date and time slots in the Upcoming tab
    val currentList = remember(rawAppointmentsList, selectedTab) {
        if (selectedTab == AppointmentTab.UPCOMING) {
            rawAppointmentsList.filter { booking ->
                BookingDateUtils.isSlotUpcoming(booking.dateStr, booking.timeSlot)
            }
        } else {
            rawAppointmentsList
        }
    }

    // Ensure appointments are loaded on entry or user change
    LaunchedEffect(selectedTab, userProfile.id) {
        viewModel.fetchAppointmentsForCurrentTab()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .testTag("bookings_tab_screen")
    ) {
        // Screen Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "My Appointments",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Real-time bookings from cloud API (${userProfile?.name ?: "Customer"})",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                Surface(
                    onClick = { viewModel.showSmartRecoveryConsole.value = true },
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("btn_bookings_smart_engine")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Engine", color = AmberGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-tabs row: Upcoming | Cancelled | Completed
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceCard)
                    .padding(4.dp)
            ) {
                listOf(
                    AppointmentTab.UPCOMING,
                    AppointmentTab.CANCELLED,
                    AppointmentTab.COMPLETED
                ).forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) AmberGold else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable { viewModel.selectAppointmentTab(tab) }
                            .padding(vertical = 10.dp)
                            .testTag("tab_${tab.statusKey.lowercase()}")
                    ) {
                        Text(
                            text = tab.displayName,
                            color = if (isSelected) ObsidianDark else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Active List or Loading State
        if (isLoading) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AmberGold, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Loading appointments from API...",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else if (errorMsg != null && currentList.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = CrimsonRed,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Unable to fetch appointments",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMsg ?: "Please verify your connection.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.fetchAppointmentsForCurrentTab() },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Retry", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (currentList.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = when (selectedTab) {
                            AppointmentTab.UPCOMING -> "No upcoming appointments"
                            AppointmentTab.CANCELLED -> "No cancelled appointments"
                            AppointmentTab.COMPLETED -> "No completed appointments yet"
                        },
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when (selectedTab) {
                            AppointmentTab.UPCOMING -> "Find a top-rated barber in your area and book your next haircut."
                            AppointmentTab.CANCELLED -> "You have no cancelled appointments."
                            AppointmentTab.COMPLETED -> "Completed appointments will appear here after your service."
                        },
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    if (selectedTab == AppointmentTab.UPCOMING) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = { viewModel.currentTab.value = 0 },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianDark),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Explore Barbers", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(currentList, key = { index, booking -> "${booking.id}_${booking.status.name}_$index" }) { _, booking ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("booking_card_${booking.id}")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header with Status Badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(
                                        text = booking.shopName,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 16.sp
                                    )
                                    if (booking.shopAddress.isNotBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = AmberGold,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = booking.shopAddress,
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    Text(
                                        text = booking.id,
                                        color = AmberGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (booking.status) {
                                        BookingStatus.UPCOMING -> EmeraldGreenBg
                                        BookingStatus.COMPLETED -> AmberGlow
                                        BookingStatus.CANCELLED -> CrimsonRedBg
                                        BookingStatus.NO_SHOW -> CrimsonRedBg
                                    }
                                ) {
                                    Text(
                                        text = booking.status.name,
                                        color = when (booking.status) {
                                            BookingStatus.UPCOMING -> EmeraldGreen
                                            BookingStatus.COMPLETED -> AmberGold
                                            BookingStatus.CANCELLED -> CrimsonRed
                                            BookingStatus.NO_SHOW -> CrimsonRed
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = BorderSubtle)
                            Spacer(modifier = Modifier.height(10.dp))

                            // 15-Minute Arrival Reminder Status Pill
                            if (booking.status == BookingStatus.UPCOMING) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (booking.reminderSentAt != null) AmberGlow else SurfaceElevated,
                                    border = BorderStroke(1.dp, if (booking.reminderSentAt != null) AmberGold.copy(alpha = 0.5f) else BorderSubtle),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (booking.reminderSentAt != null) {
                                                viewModel.openReminderDetails(booking)
                                            } else {
                                                viewModel.trigger15MinuteReminder(booking.id)
                                            }
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Alarm,
                                                contentDescription = null,
                                                tint = if (booking.reminderSentAt != null) AmberGold else TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (booking.reminderSentAt != null) "15-Min Arrival Reminder Sent" else "Schedule 15-Min Arrival Reminder",
                                                color = if (booking.reminderSentAt != null) AmberGold else TextSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = if (booking.reminderSentAt != null) "View ➔" else "Trigger ➔",
                                            color = AmberGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))

                                // Real-Time Waiting Customer Count & Queue Status Card
                                RealtimeQueueWaitingCard(
                                    booking = booking,
                                    viewModel = viewModel,
                                    onViewLiveQueue = { viewModel.openLiveQueue(booking) }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // Details
                            Text(
                                text = "Services: " + booking.serviceNames.joinToString(", "),
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = AmberGold, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = BookingDateUtils.formatUserFriendlyDateTime(booking.dateStr, booking.timeSlot),
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(Icons.Default.Person, contentDescription = null, tint = AmberGold, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = booking.barberName,
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Total Paid: ₹${booking.total}",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AmberGold,
                                    fontSize = 15.sp
                                )

                                // Action Buttons depending on status
                                when (booking.status) {
                                    BookingStatus.UPCOMING -> {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.noShowModalBooking.value = booking
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, TextTertiary.copy(alpha = 0.5f)),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text("No-Show", color = TextSecondary, fontSize = 11.sp)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.cancelModalBooking.value = booking
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.6f)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("Cancel", color = CrimsonRed, fontSize = 11.sp)
                                            }

                                            Button(
                                                onClick = {
                                                    viewModel.rescheduleModalBooking.value = booking
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianDark),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("Reschedule", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    BookingStatus.COMPLETED -> {
                                        Button(
                                            onClick = {
                                                viewModel.reviewModalBooking.value = booking
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianDark),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Rate Service", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    BookingStatus.CANCELLED -> {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Refund completed",
                                                color = TextTertiary,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = "Slot recovered & offered",
                                                color = EmeraldGreen,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    BookingStatus.NO_SHOW -> {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Marked No-Show by Shop",
                                                color = CrimsonRed,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Slot recovered for flash booking",
                                                color = AmberGold,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
