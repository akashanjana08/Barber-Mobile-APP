package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.ui.BarberViewModel
import com.example.ui.theme.*

/**
 * Compact real-time queue waiting card displayed inside the appointment card.
 */
@Composable
fun RealtimeQueueWaitingCard(
    booking: Booking,
    viewModel: BarberViewModel,
    modifier: Modifier = Modifier,
    onViewLiveQueue: () -> Unit
) {
    val queuePosition by viewModel.getQueuePositionForBooking(booking)
        .collectAsState(initial = null)
    val transportMode by viewModel.transportMode.collectAsState()

    val countBefore = queuePosition?.customersBeforeCount ?: 2
    val waitMinutes = queuePosition?.estimatedWaitingMinutes ?: 30
    val activeCustomer = queuePosition?.activeCustomer
    val isYourTurn = queuePosition?.isYourTurn ?: false

    // Pulsing live sync dot animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceElevated,
        border = BorderStroke(1.dp, if (isYourTurn) EmeraldGreen else AmberGold.copy(alpha = 0.6f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("realtime_queue_card_${booking.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Barber queue title + Live WebSocket Pulse Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = AmberGold,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = ObsidianDark,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${booking.barberName}'s Live Queue",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                }

                // Transport Badge (WebSocket / SSE / Polling)
                Surface(
                    onClick = { viewModel.toggleTransportMode() },
                    shape = RoundedCornerShape(20.dp),
                    color = Color(transportMode.badgeColor).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(transportMode.badgeColor).copy(alpha = 0.6f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(transportMode.badgeColor).copy(alpha = pulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = when (transportMode) {
                                RealtimeTransportMode.WEBSOCKET -> "Live WS"
                                RealtimeTransportMode.SSE -> "SSE"
                                RealtimeTransportMode.POLLING -> "Poll 3s"
                            },
                            color = Color(transportMode.badgeColor),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Metrics Highlight Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceDark)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Metric 1: Customers before you
                Column(modifier = Modifier.weight(1.1f)) {
                    Text(
                        text = "CUSTOMERS BEFORE YOU",
                        color = TextTertiary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (countBefore == 0) {
                            Text(
                                text = "You're up next! 🎉",
                                color = EmeraldGreen,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        } else {
                            Text(
                                text = "$countBefore",
                                color = AmberGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (countBefore == 1) "customer ahead" else "customers ahead",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .width(1.dp)
                        .background(BorderSubtle)
                )

                // Metric 2: Estimated waiting time based on actual service duration
                Column(
                    modifier = Modifier
                        .weight(0.9f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = "ESTIMATED WAIT",
                        color = TextTertiary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (waitMinutes == 0) EmeraldGreen else AmberGold,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (waitMinutes == 0) "Immediate" else "~$waitMinutes mins",
                            color = if (waitMinutes == 0) EmeraldGreen else TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Active Customer in Chair Sub-text
            if (activeCustomer != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCut,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Now in chair: ${activeCustomer.customerName} (${activeCustomer.serviceNames.firstOrNull() ?: "Service"} • ~${activeCustomer.remainingMinutes}m left)",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row: View Live Queue & Quick Test Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Updates in real time as services complete",
                    color = TextTertiary,
                    fontSize = 10.sp
                )

                Button(
                    onClick = onViewLiveQueue,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberGold,
                        contentColor = ObsidianDark
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("btn_view_live_queue_${booking.id}")
                ) {
                    Text(
                        text = "View Live Queue ➔",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Full Interactive Live Barber Queue Timeline & Simulation Modal.
 */
@Composable
fun RealtimeQueueModalDialog(
    booking: Booking,
    viewModel: BarberViewModel,
    onDismiss: () -> Unit
) {
    val queuePosition by viewModel.getQueuePositionForBooking(booking)
        .collectAsState(initial = null)
    val barberQueue by viewModel.getBarberQueueFlow(booking.barberName)
        .collectAsState(initial = emptyList())
    val transportMode by viewModel.transportMode.collectAsState()
    val latestEvent by viewModel.latestQueueEvent.collectAsState()

    var showSimControls by remember { mutableStateOf(true) }
    var walkInName by remember { mutableStateOf("Walk-in Customer") }

    val countBefore = queuePosition?.customersBeforeCount ?: 0
    val waitMinutes = queuePosition?.estimatedWaitingMinutes ?: 0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianDark)
                .testTag("realtime_queue_modal"),
            color = ObsidianDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // Modal Top Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "Real-Time Barber Queue",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "${booking.barberName} • ${booking.shopName}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Network Transport Mode Switcher
                    Surface(
                        onClick = { viewModel.toggleTransportMode() },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(transportMode.badgeColor).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(transportMode.badgeColor).copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.Wifi,
                                contentDescription = null,
                                tint = Color(transportMode.badgeColor),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = transportMode.label,
                                color = Color(transportMode.badgeColor),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Big Queue Stats Banner
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text(
                                            text = "YOUR APPOINTMENT",
                                            color = TextTertiary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = "${booking.timeSlot} • Today",
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TextPrimary,
                                            fontSize = 18.sp
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (countBefore == 0) EmeraldGreenBg else AmberGlow
                                    ) {
                                        Text(
                                            text = if (countBefore == 0) "UP NEXT 🚀" else "IN QUEUE",
                                            color = if (countBefore == 0) EmeraldGreen else AmberGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(SurfaceDark)
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "$countBefore",
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (countBefore == 0) EmeraldGreen else AmberGold
                                        )
                                        Text(
                                            text = if (countBefore == 1) "customer ahead" else "customers ahead",
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .height(40.dp)
                                            .width(1.dp)
                                            .background(BorderSubtle)
                                    )

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = if (waitMinutes == 0) "0" else "~$waitMinutes",
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (waitMinutes == 0) EmeraldGreen else AmberGold
                                        )
                                        Text(
                                            text = "minutes wait time",
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                if (latestEvent != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SurfaceElevated,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Bolt,
                                                contentDescription = null,
                                                tint = AmberGold,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = latestEvent!!.message,
                                                color = TextPrimary,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Interactive Real-Time Queue Simulation Panel
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.TouchApp,
                                            contentDescription = null,
                                            tint = AmberGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Real-Time Event Simulator",
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 14.sp
                                        )
                                    }

                                    TextButton(onClick = { showSimControls = !showSimControls }) {
                                        Text(
                                            text = if (showSimControls) "Hide" else "Show",
                                            color = AmberGold,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = showSimControls) {
                                    Column {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Trigger real-time queue events to observe instant waiting count & wait duration recalculation:",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Simulation Action Buttons Grid
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Complete Current Service
                                            OutlinedButton(
                                                onClick = {
                                                    val active = barberQueue.find { it.status == QueueCustomerStatus.IN_PROGRESS }
                                                    if (active != null) {
                                                        viewModel.completeBarberService(active.id)
                                                    } else {
                                                        val first = barberQueue.firstOrNull { it.status == QueueCustomerStatus.CHECKED_IN || it.status == QueueCustomerStatus.CONFIRMED }
                                                        if (first != null) viewModel.startBarberService(first.id)
                                                    }
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.6f)),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Barber: Finish", color = EmeraldGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }

                                            // Customer Ahead Cancels
                                            OutlinedButton(
                                                onClick = {
                                                    val ahead = barberQueue.find { !it.isCurrentUser && it.status.isRelevant() && it.id != "Q_RAHUL_CURRENT" }
                                                    if (ahead != null) {
                                                        viewModel.cancelQueueCustomer(ahead.id)
                                                    }
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.6f)),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                                            ) {
                                                Icon(Icons.Default.Cancel, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Ahead Cancels", color = CrimsonRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Customer Ahead No-Show
                                            OutlinedButton(
                                                onClick = {
                                                    val ahead = barberQueue.find { !it.isCurrentUser && it.status.isRelevant() && it.id != "Q_RAHUL_CURRENT" }
                                                    if (ahead != null) {
                                                        viewModel.markQueueCustomerNoShow(ahead.id)
                                                    }
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, TextTertiary),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                                            ) {
                                                Icon(Icons.Default.PersonOff, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Mark No-Show", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }

                                            // Broadcast 10-Minute Delay
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.broadcastBarberDelay(booking.barberName, 10)
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.6f)),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                                            ) {
                                                Icon(Icons.Default.MoreTime, contentDescription = null, tint = AmberGold, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("+10 Min Delay", color = AmberGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Customer Check In
                                            OutlinedButton(
                                                onClick = {
                                                    val confirmed = barberQueue.find { it.status == QueueCustomerStatus.CONFIRMED }
                                                    if (confirmed != null) {
                                                        viewModel.checkInQueueCustomer(confirmed.id)
                                                    }
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, SkyBlue.copy(alpha = 0.6f)),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                                            ) {
                                                Icon(Icons.Default.WhereToVote, contentDescription = null, tint = SkyBlue, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Check In Ahead", color = SkyBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }

                                            // Reset Benchmark
                                            OutlinedButton(
                                                onClick = { viewModel.resetQueueBenchmark() },
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, BorderSubtle),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                                            ) {
                                                Icon(Icons.Default.RestartAlt, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Reset (2 Ahead)", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section Title: Chronological Queue Timeline
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Queue Timeline for ${booking.barberName}",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "${barberQueue.size} scheduled today",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Timeline Items
                    items(barberQueue, key = { it.id }) { item ->
                        val isCurrent = item.isCurrentUser || item.bookingId == booking.id
                        val isAhead = !isCurrent && item.status.isRelevant() && (item.scheduledTimeMinutes < parseTimeToMinutesOfDay(booking.timeSlot) || item.status == QueueCustomerStatus.IN_PROGRESS)

                        QueueTimelineItemCard(
                            item = item,
                            isCurrentTargetUser = isCurrent,
                            isCustomerAhead = isAhead,
                            onStart = { viewModel.startBarberService(item.id) },
                            onComplete = { viewModel.completeBarberService(item.id) },
                            onCheckIn = { viewModel.checkInQueueCustomer(item.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual Timeline Card in the Queue.
 */
@Composable
fun QueueTimelineItemCard(
    item: BarberQueueItem,
    isCurrentTargetUser: Boolean,
    isCustomerAhead: Boolean,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    onCheckIn: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCurrentTargetUser -> AmberGlow
                item.status == QueueCustomerStatus.IN_PROGRESS -> SurfaceElevated
                else -> SurfaceCard
            }
        ),
        border = BorderStroke(
            1.dp,
            when {
                isCurrentTargetUser -> AmberGold
                item.status == QueueCustomerStatus.IN_PROGRESS -> EmeraldGreen
                isCustomerAhead -> AmberGold.copy(alpha = 0.5f)
                else -> BorderSubtle
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Time Tag
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceDark
                    ) {
                        Text(
                            text = item.scheduledTime,
                            fontWeight = FontWeight.Bold,
                            color = AmberGold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = item.customerName,
                        fontWeight = if (isCurrentTargetUser) FontWeight.ExtraBold else FontWeight.Bold,
                        color = if (isCurrentTargetUser) AmberGold else TextPrimary,
                        fontSize = 14.sp
                    )

                    if (isCurrentTargetUser) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = AmberGold
                        ) {
                            Text(
                                text = "YOU",
                                color = ObsidianDark,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (item.status) {
                        QueueCustomerStatus.IN_PROGRESS -> EmeraldGreenBg
                        QueueCustomerStatus.CHECKED_IN -> SkyBlueBg
                        QueueCustomerStatus.CONFIRMED -> SurfaceDark
                        QueueCustomerStatus.COMPLETED -> TextTertiary.copy(alpha = 0.2f)
                        QueueCustomerStatus.CANCELLED -> CrimsonRedBg
                        QueueCustomerStatus.NO_SHOW -> CrimsonRedBg
                        QueueCustomerStatus.DELAYED -> AmberGlow
                        QueueCustomerStatus.EXPIRED -> SurfaceDark
                    }
                ) {
                    Text(
                        text = when (item.status) {
                            QueueCustomerStatus.IN_PROGRESS -> "IN CHAIR ✂️"
                            QueueCustomerStatus.CHECKED_IN -> "CHECKED IN 📍"
                            QueueCustomerStatus.CONFIRMED -> "SCHEDULED"
                            QueueCustomerStatus.COMPLETED -> "COMPLETED ✓"
                            QueueCustomerStatus.CANCELLED -> "CANCELLED"
                            QueueCustomerStatus.NO_SHOW -> "NO SHOW"
                            QueueCustomerStatus.DELAYED -> "DELAYED"
                            QueueCustomerStatus.EXPIRED -> "EXPIRED"
                        },
                        color = when (item.status) {
                            QueueCustomerStatus.IN_PROGRESS -> EmeraldGreen
                            QueueCustomerStatus.CHECKED_IN -> SkyBlue
                            QueueCustomerStatus.CONFIRMED -> TextSecondary
                            QueueCustomerStatus.COMPLETED -> TextTertiary
                            QueueCustomerStatus.CANCELLED -> CrimsonRed
                            QueueCustomerStatus.NO_SHOW -> CrimsonRed
                            QueueCustomerStatus.DELAYED -> AmberGold
                            QueueCustomerStatus.EXPIRED -> TextTertiary
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Service details and queue relation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${item.serviceNames.joinToString(", ")} (${item.durationMinutes}m)",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                if (isCustomerAhead) {
                    Text(
                        text = "Ahead of you in queue",
                        color = AmberGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else if (!isCurrentTargetUser && item.status.isRelevant()) {
                    Text(
                        text = "Scheduled after you",
                        color = TextTertiary,
                        fontSize = 10.sp
                    )
                }
            }

            if (item.status == QueueCustomerStatus.IN_PROGRESS) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { 0.5f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = EmeraldGreen,
                    trackColor = SurfaceDark
                )
            }
        }
    }
}
