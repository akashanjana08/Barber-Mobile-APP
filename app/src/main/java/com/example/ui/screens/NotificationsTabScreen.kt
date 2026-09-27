package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NotificationType
import com.example.ui.BarberViewModel
import com.example.ui.theme.*

@Composable
fun NotificationsTabScreen(
    viewModel: BarberViewModel,
    modifier: Modifier = Modifier
) {
    val notifications by viewModel.notifications.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .testTag("notifications_tab_screen")
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Notification Center",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "Arrival reminders, cancelled slot offers & alerts",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = { viewModel.showSmartRecoveryConsole.value = true },
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("btn_alerts_engine_console")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = AmberGold, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Engine", color = AmberGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (notifications.any { !it.isRead }) {
                    Spacer(modifier = Modifier.width(6.dp))
                    TextButton(onClick = { viewModel.markAllNotificationsRead() }) {
                        Text("Mark Read", color = AmberGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (notifications.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsNone,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "You're all caught up!",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "No new alerts or reminders right now.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(notifications, key = { it.id }) { notif ->
                    val (icon, iconColor, bgIconColor) = when (notif.type) {
                        NotificationType.CONFIRMED -> Triple(Icons.Default.CheckCircle, EmeraldGreen, EmeraldGreenBg)
                        NotificationType.REMINDER -> Triple(Icons.Default.Alarm, AmberGold, AmberGlow)
                        NotificationType.CANCELLED -> Triple(Icons.Default.Cancel, CrimsonRed, CrimsonRedBg)
                        NotificationType.RESCHEDULED -> Triple(Icons.Default.Update, SkyBlue, SkyBlueBg)
                        NotificationType.OFFER -> Triple(Icons.Default.LocalFireDepartment, CrimsonRed, CrimsonRedBg)
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (notif.isRead) SurfaceCard else SurfaceElevated
                        ),
                        border = BorderStroke(1.dp, if (notif.isRead) BorderSubtle else AmberGold.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                when (notif.type) {
                                    NotificationType.REMINDER -> {
                                        viewModel.openReminderDetailsById(notif.relatedBookingId ?: "BK10234")
                                    }
                                    NotificationType.OFFER -> {
                                        viewModel.openOfferDetailsById(notif.relatedOfferId ?: "OFFER_ROYAL_530")
                                    }
                                    else -> {
                                        if (notif.relatedBookingId != null) {
                                            viewModel.openReminderDetailsById(notif.relatedBookingId)
                                        }
                                    }
                                }
                            }
                            .testTag("notif_card_${notif.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = bgIconColor,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = iconColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = notif.title,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = notif.timeAgo,
                                            color = TextTertiary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = notif.message,
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }

                            // Interactive Pill for specific notification action
                            if (notif.type == NotificationType.REMINDER) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AmberGlow,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.openReminderDetailsById(notif.relatedBookingId ?: "BK10234") }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("⏰ View Appointment & Get Directions", color = AmberGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("Tap ➔", color = AmberGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else if (notif.type == NotificationType.OFFER) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = CrimsonRedBg,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.openOfferDetailsById(notif.relatedOfferId ?: "OFFER_ROYAL_530") }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("🔥 View & Book Recovered Slot (20% Off)", color = CrimsonRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("Book Now ➔", color = CrimsonRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
