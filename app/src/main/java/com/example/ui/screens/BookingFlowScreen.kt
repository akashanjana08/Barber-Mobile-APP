package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.network.QuickQueuePreviewDto
import com.example.data.util.BookingDateUtils
import com.example.ui.BarberViewModel
import com.example.ui.QuickQueueUiState
import com.example.ui.components.BookingDetailDialog
import com.example.ui.components.CancelBookingDialog
import com.example.ui.components.CountdownBadge
import com.example.ui.components.RescheduleDialog
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookingFlowScreen(
    viewModel: BarberViewModel,
    modifier: Modifier = Modifier
) {
    val shop = viewModel.selectedShop.collectAsState().value ?: return
    val step by viewModel.bookingStep.collectAsState()
    val selectedMode by viewModel.selectedBookingMode.collectAsState()
    val quickQueueState by viewModel.quickQueueUiState.collectAsState()
    val isRequestingQuickQueue by viewModel.isRequestingQuickQueue.collectAsState()
    val selectedServices by viewModel.selectedServices.collectAsState()
    val selectedBarber by viewModel.selectedBarber.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedSlot by viewModel.selectedSlot.collectAsState()
    val availableSlotsMap by viewModel.availableSlotsMap.collectAsState()
    val isLoadingSlots by viewModel.isLoadingSlots.collectAsState()
    val isHoldingSlot by viewModel.isHoldingSlot.collectAsState()
    val holdCountdown by viewModel.holdCountdownSeconds.collectAsState()
    val conflictError by viewModel.bookingConflictError.collectAsState()
    val simulateConflict by viewModel.simulateConflictMode.collectAsState()
    val isProcessingPayment by viewModel.isProcessingPayment.collectAsState()
    val confirmedBooking by viewModel.lastConfirmedBooking.collectAsState()
    val availableDates by viewModel.availableBookingDates.collectAsState()
    val slotIntervalMinutes by viewModel.slotIntervalMinutes.collectAsState()

    var showDetailDialog by remember { mutableStateOf(false) }
    val cancelBookingTarget by viewModel.cancelModalBooking.collectAsState()
    val rescheduleBookingTarget by viewModel.rescheduleModalBooking.collectAsState()

    val context = LocalContext.current

    val subtotal = selectedServices.sumOf { it.price }
    val totalDurationMin = selectedServices.sumOf { it.durationMin }
    val tax = 18
    val grandTotal = subtotal + tax

    // Native Android Back Navigation
    BackHandler {
        if (step > 1 && step < 6) {
            viewModel.bookingStep.value = step - 1
        } else {
            viewModel.closeSubScreens()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .statusBarsPadding()
            .testTag("booking_flow_screen")
    ) {
        // Top App Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            IconButton(
                onClick = {
                    if (step > 1 && step < 6) {
                        viewModel.bookingStep.value = step - 1
                    } else {
                        viewModel.closeSubScreens()
                    }
                },
                modifier = Modifier.testTag("booking_flow_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        step == 6 -> "Booking Confirmed"
                        selectedMode == BookingMode.QUICK_QUEUE && step == 1 -> "Quick Queue • Select Services"
                        selectedMode == BookingMode.QUICK_QUEUE && step == 2 -> "Quick Queue • Earliest Slot"
                        step == 1 -> "Step 1 of 4: Select Services"
                        step == 2 -> "Step 2 of 4: Choose Stylist"
                        step == 3 -> "Step 3 of 4: Date & Availability"
                        step == 4 -> "Step 4 of 4: Review & Reserve"
                        else -> "Book Appointment"
                    },
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 15.sp
                )
                Text(
                    text = shop.name,
                    color = AmberGold,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = { viewModel.closeSubScreens() },
                modifier = Modifier.testTag("booking_flow_close_button")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
            }
        }

        // Linear Progress bar (Steps 1 to 4)
        if (step in 1..4) {
            LinearProgressIndicator(
                progress = {
                    if (selectedMode == BookingMode.QUICK_QUEUE) {
                        if (step == 1) 0.5f else 1.0f
                    } else {
                        step / 4f
                    }
                },
                color = AmberGold,
                trackColor = SurfaceElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
            )
        }

        // Conflict Error Banner
        if (conflictError != null) {
            Surface(
                color = CrimsonRedBg,
                border = BorderStroke(1.dp, CrimsonRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .testTag("booking_conflict_banner")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "This time slot is no longer available",
                            color = CrimsonRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Please refresh availability and select another option.",
                            color = TextPrimary,
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.bookingStep.value = 3
                            viewModel.fetchAvailability()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Refresh", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Content Area
        Box(modifier = Modifier.weight(1f)) {
            when {
                // =============================================================
                // STEP 1: MODE SELECTION & SERVICE SELECTION
                // =============================================================
                step == 1 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(20.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Section 3: Booking Mode Selection Header
                        item {
                            Text(
                                text = "Select Booking Mode",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Choose how you would like to book your salon appointment.",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            // Option A: Quick Queue Booking Card
                            BookingModeCard(
                                title = "Quick Queue",
                                description = "Get the earliest available appointment time.",
                                supportingText = "We'll find a suitable time based on your selected services and barber availability.",
                                isSelected = selectedMode == BookingMode.QUICK_QUEUE,
                                icon = Icons.Outlined.Bolt,
                                badgeText = "⚡ FASTEST QUEUE",
                                buttonText = "Find Earliest Time",
                                onSelect = { viewModel.selectBookingMode(BookingMode.QUICK_QUEUE) },
                                onActionClick = {
                                    viewModel.selectBookingMode(BookingMode.QUICK_QUEUE)
                                    viewModel.bookingStep.value = 2
                                    viewModel.requestQuickQueuePreview()
                                },
                                testTag = "quick_queue_mode_card"
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Option B: Scheduled Booking Card
                            BookingModeCard(
                                title = "Schedule Appointment",
                                description = "Choose a date and time that works for you.",
                                supportingText = "Select your preferred appointment slot.",
                                isSelected = selectedMode == BookingMode.SCHEDULED,
                                icon = Icons.Outlined.CalendarMonth,
                                badgeText = "📅 CUSTOM SLOT",
                                buttonText = "Choose Date & Time",
                                onSelect = { viewModel.selectBookingMode(BookingMode.SCHEDULED) },
                                onActionClick = {
                                    viewModel.selectBookingMode(BookingMode.SCHEDULED)
                                    viewModel.bookingStep.value = 2
                                },
                                testTag = "scheduled_mode_card"
                            )

                            Spacer(modifier = Modifier.height(24.dp))
                            HorizontalDivider(color = BorderSubtle)
                            Spacer(modifier = Modifier.height(16.dp))

                            // Services Header
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(
                                        text = "Select Grooming Services",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Text(
                                        text = "Multi-service selections supported",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                Surface(shape = RoundedCornerShape(8.dp), color = SurfaceElevated) {
                                    Text(
                                        text = "${selectedServices.size} selected",
                                        color = AmberGold,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // List of available services
                        items(shop.services, key = { it.id }) { service ->
                            val isSelected = selectedServices.any { it.id == service.id }
                            Surface(
                                onClick = { viewModel.toggleServiceSelection(service) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) SurfaceElevated else SurfaceCard,
                                border = BorderStroke(1.dp, if (isSelected) AmberGold else BorderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .testTag("service_item_${service.id}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { viewModel.toggleServiceSelection(service) },
                                        colors = CheckboxDefaults.colors(checkedColor = AmberGold, checkmarkColor = ObsidianDark)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(service.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                                        Text("${service.durationMin} mins • ${service.category}", color = TextSecondary, fontSize = 12.sp)
                                    }
                                    Text("₹${service.price}", fontWeight = FontWeight.ExtraBold, color = AmberGold, fontSize = 15.sp)
                                }
                            }
                        }

                        // Stylist preference if in Quick Queue mode
                        if (selectedMode == BookingMode.QUICK_QUEUE) {
                            item {
                                Spacer(modifier = Modifier.height(18.dp))
                                Text(
                                    text = "Stylist Preference",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "Choose a specific barber or let the queue engine auto-assign for earliest availability.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                val isAny = selectedBarber == null
                                Surface(
                                    onClick = { viewModel.selectBarber(null) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isAny) SurfaceElevated else SurfaceCard,
                                    border = BorderStroke(1.dp, if (isAny) AmberGold else BorderSubtle),
                                    modifier = Modifier.fillMaxWidth().testTag("qq_barber_any")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        RadioButton(
                                            selected = isAny,
                                            onClick = { viewModel.selectBarber(null) },
                                            colors = RadioButtonDefaults.colors(selectedColor = AmberGold)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Any Available Barber", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                            Text("Recommended for earliest appointment time", color = EmeraldGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }

                                shop.barbers.forEach { barber ->
                                    val isSelected = selectedBarber?.id == barber.id
                                    Surface(
                                        onClick = { viewModel.selectBarber(barber) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) SurfaceElevated else SurfaceCard,
                                        border = BorderStroke(1.dp, if (isSelected) AmberGold else BorderSubtle),
                                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp).testTag("qq_barber_${barber.id}")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { viewModel.selectBarber(barber) },
                                                colors = RadioButtonDefaults.colors(selectedColor = AmberGold)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(barber.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                                Text("${barber.specialty} • ${barber.experienceYears} yrs exp", color = TextSecondary, fontSize = 11.sp)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Star, contentDescription = null, tint = StarYellow, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("${barber.rating}", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // =============================================================
                // STEP 2 (QUICK QUEUE): EARLIEST TIME GENERATED BY BACKEND
                // =============================================================
                selectedMode == BookingMode.QUICK_QUEUE && step == 2 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(20.dp),
                        modifier = Modifier.fillMaxSize().testTag("quick_queue_result_screen")
                    ) {
                        item {
                            Text(
                                text = "Quick Queue Earliest Time",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Generated directly by salon backend availability engine.",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Loading State
                        if (isRequestingQuickQueue || quickQueueState is QuickQueueUiState.LoadingPreview) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth().padding(36.dp)
                                    ) {
                                        CircularProgressIndicator(color = AmberGold, strokeWidth = 3.dp)
                                        Spacer(modifier = Modifier.height(18.dp))
                                        Text(
                                            text = "Finding Earliest Time Slot...",
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Querying live barber schedule and active salon queues...",
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        // No Availability State
                        if (quickQueueState is QuickQueueUiState.NoAvailability) {
                            val noAvailMsg = (quickQueueState as QuickQueueUiState.NoAvailability).message
                            item {
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = CrimsonRedBg),
                                    border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth().testTag("quick_queue_no_availability_card")
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth().padding(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EventBusy,
                                            contentDescription = null,
                                            tint = CrimsonRed,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "No Earliest Time Available",
                                            fontWeight = FontWeight.Bold,
                                            color = CrimsonRed,
                                            fontSize = 16.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = noAvailMsg,
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(20.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = { viewModel.bookingStep.value = 1 },
                                                border = BorderStroke(1.dp, BorderSubtle),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Change Services", color = TextPrimary, fontSize = 12.sp)
                                            }
                                            Button(
                                                onClick = {
                                                    viewModel.selectBookingMode(BookingMode.SCHEDULED)
                                                    viewModel.bookingStep.value = 2
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianDark),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Switch to Scheduled", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Proposal Changed Warning Banner
                        if (quickQueueState is QuickQueueUiState.ProposalChanged) {
                            val changeMsg = (quickQueueState as QuickQueueUiState.ProposalChanged).message
                            item {
                                Surface(
                                    color = AmberGlow,
                                    border = BorderStroke(1.dp, AmberGold),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = AmberGold, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(text = changeMsg, color = TextPrimary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Generated Confirmation Card (Backend-Generated Time)
                        val proposal = when (val state = quickQueueState) {
                            is QuickQueueUiState.Proposed -> state.proposal
                            is QuickQueueUiState.ProposalChanged -> state.updatedProposal
                            else -> null
                        }

                        if (proposal != null) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth().testTag("quick_queue_confirmation_card")
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        // Header Badge
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = EmeraldGreenBg,
                                                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f))
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Backend-Generated Earliest Time", color = EmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                            }

                                            Text(
                                                text = "${proposal.totalDurationMinutes} mins",
                                                color = AmberGold,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Big Generated Time Display
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = SurfaceElevated,
                                            border = BorderStroke(1.dp, BorderSubtle),
                                            modifier = Modifier.fillMaxWidth().testTag("quick_queue_time_card")
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.padding(16.dp)
                                            ) {
                                                Text(
                                                    text = "Available Appointment Times",
                                                    color = TextSecondary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                val friendlyTimeRange = BookingDateUtils.formatUserFriendlyTimeRange(proposal.startTime, proposal.endTime)
                                                Text(
                                                    text = friendlyTimeRange.ifBlank { "10:30 AM – 11:15 AM" },
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = AmberGold,
                                                    fontSize = 22.sp,
                                                    modifier = Modifier.testTag("quick_queue_appointment_time")
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                val friendlyDate = BookingDateUtils.formatUserFriendlyDate(proposal.dateStr ?: selectedDate)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = friendlyDate,
                                                        color = TextPrimary,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        modifier = Modifier.testTag("quick_queue_appointment_date")
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        // Alternative Quick Queue Available Times
                                        val duration = if (proposal.totalDurationMinutes > 0) proposal.totalDurationMinutes else 45
                                        val alternativeTimes = remember(proposal.startTime, duration) {
                                            val list = mutableListOf<Pair<String, String>>()
                                            val baseStart = BookingDateUtils.formatUserFriendlyTime(proposal.startTime).ifBlank { "10:30 AM" }
                                            val firstStart = if (baseStart.contains(" – ")) baseStart.substringBefore(" – ") else baseStart
                                            val t1 = BookingDateUtils.deriveExpectedEndTime(firstStart, 60)
                                            val e1 = BookingDateUtils.deriveExpectedEndTime(t1, duration)
                                            val t2 = BookingDateUtils.deriveExpectedEndTime(t1, 60)
                                            val e2 = BookingDateUtils.deriveExpectedEndTime(t2, duration)
                                            val t3 = BookingDateUtils.deriveExpectedEndTime(t2, 60)
                                            val e3 = BookingDateUtils.deriveExpectedEndTime(t3, duration)
                                            if (t1.isNotBlank() && e1.isNotBlank()) list.add(t1 to e1)
                                            if (t2.isNotBlank() && e2.isNotBlank()) list.add(t2 to e2)
                                            if (t3.isNotBlank() && e3.isNotBlank()) list.add(t3 to e3)
                                            list
                                        }

                                        if (alternativeTimes.isNotEmpty()) {
                                            Text(
                                                text = "Other Available Times Today:",
                                                color = TextSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                                            ) {
                                                alternativeTimes.forEach { (altStart, altEnd) ->
                                                    Surface(
                                                        onClick = {
                                                            val updated = proposal.copy(startTime = altStart, endTime = altEnd)
                                                            viewModel.activeQuickQueueProposal.value = updated
                                                            viewModel.quickQueueUiState.value = QuickQueueUiState.Proposed(updated)
                                                            viewModel.selectedSlot.value = altStart
                                                        },
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = SurfaceElevated,
                                                        border = BorderStroke(1.dp, BorderSubtle),
                                                        modifier = Modifier.testTag("alt_time_${altStart.replace(" ", "_")}")
                                                    ) {
                                                        Text(
                                                            text = BookingDateUtils.formatUserFriendlyTimeRange(altStart, altEnd),
                                                            color = AmberGold,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Shop and Barber Details
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text("Salon:", color = TextSecondary, fontSize = 13.sp)
                                            Text(proposal.shopName ?: shop.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text("Stylist:", color = TextSecondary, fontSize = 13.sp)
                                            Text(proposal.barberName ?: (selectedBarber?.name ?: "Assigned Master Stylist"), fontWeight = FontWeight.Bold, color = AmberGold, fontSize = 13.sp)
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider(color = BorderSubtle)
                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Selected Services
                                        Text("Selected Services:", color = TextSecondary, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        selectedServices.forEach { s ->
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                            ) {
                                                Text("• ${s.name} (${s.durationMin}m)", color = TextPrimary, fontSize = 13.sp)
                                                Text("₹${s.price}", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider(color = BorderSubtle)
                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Total Price
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text("Total Price", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                                            val displayPrice = if (proposal.price > 0) proposal.price else grandTotal
                                            Text("₹$displayPrice", fontWeight = FontWeight.ExtraBold, color = AmberGold, fontSize = 18.sp)
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        // Booking & Payment Info Notice
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = EmeraldGreenBg,
                                            border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(10.dp)
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text("Instant Server Reservation", color = EmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    Text("Slot confirmed immediately. Pay at salon post-service.", color = TextSecondary, fontSize = 11.sp)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(20.dp))

                                        // Confirm Booking Button
                                        Button(
                                            onClick = { viewModel.confirmQuickQueueProposal() },
                                            enabled = !isRequestingQuickQueue,
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianDark),
                                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("quick_queue_confirm_button")
                                        ) {
                                            if (isRequestingQuickQueue) {
                                                CircularProgressIndicator(color = ObsidianDark, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                            } else {
                                                Text("Confirm Booking", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedButton(
                                            onClick = { viewModel.bookingStep.value = 1 },
                                            border = BorderStroke(1.dp, BorderSubtle),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth().height(44.dp)
                                        ) {
                                            Text("Change Services / Barber", color = TextSecondary, fontSize = 13.sp)
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        TextButton(
                                            onClick = {
                                                viewModel.selectBookingMode(BookingMode.SCHEDULED)
                                                viewModel.bookingStep.value = 3
                                            },
                                            modifier = Modifier.fillMaxWidth().testTag("qq_choose_different_slot_btn")
                                        ) {
                                            Text("Choose a Different Date or Time Slot ➔", color = AmberGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // =============================================================
                // STEP 2 (SCHEDULED): CHOOSE BARBER
                // =============================================================
                selectedMode == BookingMode.SCHEDULED && step == 2 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(20.dp),
                        modifier = Modifier.fillMaxSize().testTag("scheduled_barber_screen")
                    ) {
                        item {
                            Text(
                                text = "Choose Your Stylist",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Choose 'Any Available Barber' for wider slot availability or pick your favourite stylist.",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val isAny = selectedBarber == null
                            Surface(
                                onClick = { viewModel.selectBarber(null) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isAny) SurfaceElevated else SurfaceCard,
                                border = BorderStroke(1.dp, if (isAny) AmberGold else BorderSubtle),
                                modifier = Modifier.fillMaxWidth().testTag("barber_any_option")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    RadioButton(
                                        selected = isAny,
                                        onClick = { viewModel.selectBarber(null) },
                                        colors = RadioButtonDefaults.colors(selectedColor = AmberGold)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Any Available Barber", fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("Maximum time slot availability", color = TextSecondary, fontSize = 12.sp)
                                    }
                                    Surface(shape = RoundedCornerShape(6.dp), color = EmeraldGreenBg) {
                                        Text("Recommended", color = EmeraldGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        items(shop.barbers, key = { it.id }) { barber ->
                            val isSelected = selectedBarber?.id == barber.id
                            Surface(
                                onClick = { viewModel.selectBarber(barber) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) SurfaceElevated else SurfaceCard,
                                border = BorderStroke(1.dp, if (isSelected) AmberGold else BorderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .testTag("barber_item_${barber.id}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.selectBarber(barber) },
                                        colors = RadioButtonDefaults.colors(selectedColor = AmberGold)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(barber.name, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("${barber.specialty} • ${barber.experienceYears} yrs exp", color = TextSecondary, fontSize = 12.sp)
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SurfaceElevated)
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = StarYellow, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("${barber.rating}", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // =============================================================
                // STEP 3 (SCHEDULED): DATE & APPOINTMENT TIME SLOTS
                // =============================================================
                selectedMode == BookingMode.SCHEDULED && step == 3 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(20.dp),
                        modifier = Modifier.fillMaxSize().testTag("scheduled_slots_screen")
                    ) {
                        item {
                            Text(
                                text = "Select Date & Time",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Real-time appointment slots provided directly by salon backend.",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Date strip (Dynamic 3-Day Upcoming Window)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                availableDates.forEach { dateItem ->
                                    val isSelected = selectedDate == dateItem.fullDateStr || selectedDate == dateItem.shortDateStr
                                    val isCurrentDate = dateItem.relativeLabel.equals("Today", ignoreCase = true) || BookingDateUtils.isToday(dateItem.fullDateStr)
                                    Surface(
                                        onClick = { viewModel.selectDate(dateItem.fullDateStr) },
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
                                            .width(114.dp)
                                            .testTag("date_chip_${dateItem.dayNumber}")
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                                        ) {
                                            if (isCurrentDate) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (isSelected) ObsidianDark else AmberGold,
                                                    modifier = Modifier.padding(bottom = 4.dp).testTag("today_badge_${dateItem.dayNumber}")
                                                ) {
                                                    Text(
                                                        text = "CURRENT DATE",
                                                        color = if (isSelected) AmberGold else ObsidianDark,
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 8.5.sp,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${dateItem.dayNumber} ${dateItem.shortMonthName}",
                                                color = if (isSelected) ObsidianDark else TextPrimary,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (isCurrentDate) "Today" else dateItem.relativeLabel,
                                                color = if (isSelected) ObsidianDark.copy(alpha = 0.85f) else if (isCurrentDate) AmberGold else TextSecondary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Slot Interval Selector (Configurable, default 30)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("slot_interval_selector")
                            ) {
                                Text("Slot Interval:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                listOf(15, 30, 45, 60).forEach { interval ->
                                    val isSelectedInterval = slotIntervalMinutes == interval
                                    Surface(
                                        onClick = { viewModel.setSlotIntervalMinutes(interval) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelectedInterval) AmberGold else SurfaceCard,
                                        border = BorderStroke(1.dp, if (isSelectedInterval) AmberGold else BorderSubtle),
                                        modifier = Modifier.testTag("interval_chip_${interval}m")
                                    ) {
                                        Text(
                                            text = "${interval}m",
                                            color = if (isSelectedInterval) ObsidianDark else TextPrimary,
                                            fontWeight = if (isSelectedInterval) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Expected End Time Derivation Box
                            if (selectedSlot != null) {
                                val expectedEndTime = deriveExpectedEndTime(selectedSlot!!, totalDurationMin)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SurfaceElevated,
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Icon(Icons.Default.Schedule, contentDescription = null, tint = AmberGold, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("Expected Slot Timing", color = TextSecondary, fontSize = 11.sp)
                                            Text(
                                                text = "${BookingDateUtils.formatUserFriendlyTime(selectedSlot)} – ${BookingDateUtils.formatUserFriendlyTime(expectedEndTime)} ($totalDurationMin mins duration)",
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Simulation 409 Conflict Toggle for Testing
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SurfaceCard,
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Simulate 409 Conflict",
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "Test handling when another customer books the slot",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Switch(
                                        checked = simulateConflict,
                                        onCheckedChange = { viewModel.simulateConflictMode.value = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = AmberGold)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        if (isLoadingSlots) {
                            item {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxWidth().padding(32.dp)
                                ) {
                                    CircularProgressIndicator(color = AmberGold)
                                }
                            }
                        } else {
                            item {
                                val isViewingCurrentDate = BookingDateUtils.isToday(selectedDate)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isViewingCurrentDate) AmberGold.copy(alpha = 0.14f) else SurfaceCard,
                                    border = BorderStroke(1.dp, if (isViewingCurrentDate) AmberGold.copy(alpha = 0.6f) else BorderSubtle),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .testTag("availability_date_status_card")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(if (isViewingCurrentDate) AmberGold else TextSecondary, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Availability: ${BookingDateUtils.formatUserFriendlyDate(selectedDate)}",
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        if (isViewingCurrentDate) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = AmberGold,
                                                modifier = Modifier.testTag("current_date_highlight_pill")
                                            ) {
                                                Text(
                                                    text = "CURRENT DATE",
                                                    color = ObsidianDark,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 9.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            availableSlotsMap.forEach { (timeOfDay, slots) ->
                                item {
                                    Text(
                                        text = "$timeOfDay Slots",
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    ) {
                                        slots.forEach { (slotTime, isAvailable) ->
                                            val isSelected = selectedSlot == slotTime
                                            Surface(
                                                onClick = {
                                                    if (isAvailable) {
                                                        viewModel.selectSlot(slotTime)
                                                    } else {
                                                        Toast.makeText(context, "This time slot is not available. Please choose an available time.", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                shape = RoundedCornerShape(10.dp),
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
                                                modifier = Modifier.width(96.dp).testTag("slot_item_${slotTime.replace(" ", "_")}")
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.padding(vertical = 10.dp)
                                                ) {
                                                    Text(
                                                        text = BookingDateUtils.formatUserFriendlyTime(slotTime),
                                                        color = when {
                                                            !isAvailable -> TextTertiary
                                                            isSelected -> ObsidianDark
                                                            else -> TextPrimary
                                                        },
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        fontSize = 12.sp
                                                    )
                                                    Text(
                                                        text = if (isAvailable) "Available" else "Unavailable",
                                                        color = when {
                                                            !isAvailable -> CrimsonRed.copy(alpha = 0.7f)
                                                            isSelected -> ObsidianDark
                                                            else -> EmeraldGreen
                                                        },
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
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

                // =============================================================
                // STEP 4 (SCHEDULED): REVIEW & CONFIRM
                // =============================================================
                selectedMode == BookingMode.SCHEDULED && step == 4 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(20.dp),
                        modifier = Modifier.fillMaxSize().testTag("scheduled_review_screen")
                    ) {
                        item {
                            Text(
                                text = "Booking Summary",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            CountdownBadge(secondsRemaining = holdCountdown)

                            Spacer(modifier = Modifier.height(16.dp))

                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text(shop.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 17.sp)
                                    Text("📍 ${shop.address}", color = TextSecondary, fontSize = 12.sp)

                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = BorderSubtle)
                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Services
                                    Text("Services Selected:", color = TextSecondary, fontSize = 12.sp)
                                    selectedServices.forEach { s ->
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                        ) {
                                            Text("• ${s.name} (${s.durationMin}m)", color = TextPrimary, fontSize = 13.sp)
                                            Text("₹${s.price}", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = BorderSubtle)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Stylist & Timing
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Stylist:", color = TextSecondary, fontSize = 13.sp)
                                        Text(selectedBarber?.name ?: "Any Available Barber", color = AmberGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Date & Time:", color = TextSecondary, fontSize = 13.sp)
                                        val expectedEnd = if (selectedSlot != null) deriveExpectedEndTime(selectedSlot!!, totalDurationMin) else ""
                                        val friendlyDate = BookingDateUtils.formatUserFriendlyDate(selectedDate)
                                        val friendlyTime = if (selectedSlot != null) {
                                            "${BookingDateUtils.formatUserFriendlyTime(selectedSlot)} – ${BookingDateUtils.formatUserFriendlyTime(expectedEnd)}"
                                        } else ""
                                        Text("$friendlyDate • $friendlyTime", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = BorderSubtle)
                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Price breakdown
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Service Subtotal", color = TextSecondary, fontSize = 13.sp)
                                        Text("₹$subtotal", color = TextPrimary, fontSize = 13.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Convenience & Taxes", color = TextSecondary, fontSize = 13.sp)
                                        Text("₹$tax", color = TextPrimary, fontSize = 13.sp)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Grand Total", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                                        Text("₹$grandTotal", fontWeight = FontWeight.ExtraBold, color = AmberGold, fontSize = 18.sp)
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = EmeraldGreenBg,
                                        border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text("Direct API Confirmation", color = EmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text("Booked with salon server. Pay in salon post-service.", color = TextSecondary, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // =============================================================
                // STEP 6: ENHANCED APPOINTMENT CONFIRMATION & STATUS
                // =============================================================
                step == 6 -> {
                    val booking = confirmedBooking
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .testTag("booking_confirmed_screen")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldGreenBg,
                            border = BorderStroke(2.dp, EmeraldGreen),
                            modifier = Modifier.size(76.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Check, contentDescription = "Confirmed", tint = EmeraldGreen, modifier = Modifier.size(42.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Booking Confirmed! 🎉",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Your appointment is confirmed with salon backend.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                // Booking ID and Booking Mode
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text("Booking Reference", color = TextSecondary, fontSize = 11.sp)
                                        Text(booking?.id ?: "BK10234", fontWeight = FontWeight.ExtraBold, color = AmberGold, fontSize = 14.sp)
                                    }

                                    val isQQ = (booking?.bookingMode ?: "").contains("QUICK", ignoreCase = true) || selectedMode == BookingMode.QUICK_QUEUE
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isQQ) EmeraldGreenBg else AmberGlow
                                    ) {
                                        Text(
                                            text = if (isQQ) "⚡ Quick Queue" else "📅 Scheduled",
                                            color = if (isQQ) EmeraldGreen else AmberGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = BorderSubtle)
                                Spacer(modifier = Modifier.height(12.dp))

                                // Shop & Barber Details
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Salon", color = TextSecondary, fontSize = 12.sp)
                                    Text(booking?.shopName ?: shop.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Stylist", color = TextSecondary, fontSize = 12.sp)
                                    Text(booking?.barberName ?: "Master Barber", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Services", color = TextSecondary, fontSize = 12.sp)
                                    Text(
                                        text = booking?.serviceNames?.joinToString(", ") ?: selectedServices.joinToString(", ") { it.name },
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Date & Time", color = TextSecondary, fontSize = 12.sp)
                                    val finalDate = booking?.dateStr ?: selectedDate
                                    val finalTime = booking?.timeSlot ?: selectedSlot
                                    Text(BookingDateUtils.formatUserFriendlyDateTime(finalDate, finalTime), fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Total & Status", color = TextSecondary, fontSize = 12.sp)
                                    Text("₹${booking?.total ?: grandTotal} • ${booking?.paymentStatus?.name ?: "PENDING"}", fontWeight = FontWeight.ExtraBold, color = EmeraldGreen, fontSize = 13.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Primary Action: View Booking
                        Button(
                            onClick = { showDetailDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianDark),
                            modifier = Modifier.fillMaxWidth().height(46.dp).testTag("btn_view_booking")
                        ) {
                            Text("View Booking Details", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // View My Appointments
                        OutlinedButton(
                            onClick = {
                                viewModel.currentTab.value = 2 // Bookings tab
                                viewModel.closeSubScreens()
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("btn_view_my_bookings")
                        ) {
                            Text("View My Appointments", color = TextPrimary)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Reschedule Booking / Cancel Booking row
                        if (booking != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                if (!booking.bookingMode.contains("QUICK", ignoreCase = true)) {
                                    OutlinedButton(
                                        onClick = { viewModel.rescheduleModalBooking.value = booking },
                                        border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(42.dp).testTag("btn_reschedule_confirmed")
                                    ) {
                                        Text("Reschedule", color = AmberGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                OutlinedButton(
                                    onClick = { viewModel.cancelModalBooking.value = booking },
                                    border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(42.dp).testTag("btn_cancel_confirmed")
                                ) {
                                    Text("Cancel Booking", color = CrimsonRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        TextButton(
                            onClick = {
                                viewModel.currentTab.value = 0
                                viewModel.closeSubScreens()
                            },
                            modifier = Modifier.testTag("btn_back_to_home")
                        ) {
                            Text("Back to Home", color = TextSecondary)
                        }
                    }
                }
            }
        }

        // =============================================================
        // BOTTOM ACTION BAR FOR STEP 1 THROUGH 4
        // =============================================================
        if (step in 1..4 && !(selectedMode == BookingMode.QUICK_QUEUE && step == 2)) {
            Surface(
                color = SurfaceDark,
                tonalElevation = 16.dp,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Column {
                        Text(
                            text = "${selectedServices.size} Service(s)",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "₹$grandTotal",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = AmberGold
                            )
                        )
                    }

                    Button(
                        onClick = {
                            when {
                                selectedMode == BookingMode.QUICK_QUEUE && step == 1 -> {
                                    viewModel.bookingStep.value = 2
                                    viewModel.requestQuickQueuePreview()
                                }
                                selectedMode == BookingMode.SCHEDULED && step == 1 -> {
                                    viewModel.bookingStep.value = 2
                                }
                                selectedMode == BookingMode.SCHEDULED && step == 2 -> {
                                    viewModel.bookingStep.value = 3
                                    viewModel.fetchAvailability()
                                }
                                selectedMode == BookingMode.SCHEDULED && step == 3 -> {
                                    viewModel.proceedToReview()
                                }
                                selectedMode == BookingMode.SCHEDULED && step == 4 -> {
                                    viewModel.skipPaymentAndConfirmBooking()
                                }
                            }
                        },
                        enabled = !isHoldingSlot && !isProcessingPayment && !isRequestingQuickQueue &&
                                selectedServices.isNotEmpty() && (step != 3 || selectedSlot != null),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberGold,
                            contentColor = ObsidianDark,
                            disabledContainerColor = SurfaceElevated
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("booking_flow_next_button")
                    ) {
                        if (isHoldingSlot || isProcessingPayment || isRequestingQuickQueue) {
                            CircularProgressIndicator(
                                color = ObsidianDark,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = when {
                                    selectedMode == BookingMode.QUICK_QUEUE && step == 1 -> "Find Earliest Time ➔"
                                    selectedMode == BookingMode.SCHEDULED && step == 1 -> "Choose Date & Time ➔"
                                    selectedMode == BookingMode.SCHEDULED && step == 2 -> "Select Date & Slots ➔"
                                    selectedMode == BookingMode.SCHEDULED && step == 3 -> "Reserve & Review ➔"
                                    selectedMode == BookingMode.SCHEDULED && step == 4 -> "Confirm & Book Now"
                                    else -> "Continue"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Dialogs for Booking Details, Reschedule, Cancel
    if (showDetailDialog && confirmedBooking != null) {
        BookingDetailDialog(
            booking = confirmedBooking!!,
            onDismiss = { showDetailDialog = false },
            onCancel = {
                showDetailDialog = false
                viewModel.cancelModalBooking.value = confirmedBooking
            }
        )
    }

    if (cancelBookingTarget != null) {
        CancelBookingDialog(
            booking = cancelBookingTarget!!,
            onDismiss = { viewModel.cancelModalBooking.value = null },
            onConfirmCancel = {
                viewModel.cancelActiveBooking(cancelBookingTarget!!.id)
                viewModel.cancelModalBooking.value = null
            }
        )
    }

    if (rescheduleBookingTarget != null) {
        RescheduleDialog(
            booking = rescheduleBookingTarget!!,
            viewModel = viewModel,
            onConfirmReschedule = { newDate, newTime, newBarber ->
                viewModel.rescheduleActiveBooking(
                    bookingId = rescheduleBookingTarget!!.id,
                    newDateStr = newDate,
                    newTimeSlot = newTime,
                    newBarberName = newBarber
                )
                viewModel.rescheduleModalBooking.value = null
            },
            onDismiss = { viewModel.rescheduleModalBooking.value = null }
        )
    }
}

/**
 * Reusable Booking Mode Selection Card (Section 3)
 */
@Composable
fun BookingModeCard(
    title: String,
    description: String,
    supportingText: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badgeText: String,
    buttonText: String,
    onSelect: () -> Unit,
    onActionClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) SurfaceElevated else SurfaceCard,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) AmberGold else BorderSubtle
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) AmberGold else SurfaceElevated,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) ObsidianDark else AmberGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 16.sp
                        )
                        Text(
                            text = description,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                RadioButton(
                    selected = isSelected,
                    onClick = onSelect,
                    colors = RadioButtonDefaults.colors(selectedColor = AmberGold)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = supportingText,
                color = TextTertiary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) AmberGlow else SurfaceElevated
                ) {
                    Text(
                        text = badgeText,
                        color = if (isSelected) AmberGold else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) AmberGold else SurfaceElevated,
                        contentColor = if (isSelected) ObsidianDark else TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("${testTag}_action_btn")
                ) {
                    Text(text = buttonText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Derives expected appointment end time from trusted service duration.
 */
private fun deriveExpectedEndTime(startTimeStr: String, durationMinutes: Int): String {
    return BookingDateUtils.deriveExpectedEndTime(startTimeStr, durationMinutes)
}
