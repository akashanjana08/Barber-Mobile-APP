package com.example.ui.screens

import android.widget.Toast
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
import com.example.ui.BarberViewModel
import com.example.ui.components.CountdownBadge
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookingFlowScreen(
    viewModel: BarberViewModel,
    modifier: Modifier = Modifier
) {
    val shop = viewModel.selectedShop.collectAsState().value ?: return
    val step by viewModel.bookingStep.collectAsState()
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
    val selectedPaymentMethod by viewModel.selectedPaymentMethod.collectAsState()

    val context = LocalContext.current
    val dates = listOf("23 Sep (Today)", "24 Sep 2026", "25 Sep 2026", "26 Sep 2026", "27 Sep 2026", "28 Sep 2026", "29 Sep 2026")

    val subtotal = selectedServices.sumOf { it.price }
    val tax = 18
    val grandTotal = subtotal + tax

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
                }
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
                    text = when (step) {
                        1 -> "Step 1 of 5: Select Services"
                        2 -> "Step 2 of 5: Select Barber"
                        3 -> "Step 3 of 5: Date & Availability"
                        4 -> "Step 4 of 5: Review Booking"
                        5 -> "Step 5 of 5: Secure Payment"
                        6 -> "Booking Confirmed"
                        else -> "Book Appointment"
                    },
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                )
                Text(
                    text = shop.name,
                    color = AmberGold,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            // Close cross
            IconButton(onClick = { viewModel.closeSubScreens() }) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
            }
        }

        // Progress bar (Steps 1 to 5)
        if (step in 1..5) {
            LinearProgressIndicator(
                progress = { step / 5f },
                color = AmberGold,
                trackColor = SurfaceElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            )
        }

        // Conflict Error Banner (Requirement 28 & 43)
        if (conflictError != null) {
            Surface(
                color = CrimsonRedBg,
                border = BorderStroke(1.dp, CrimsonRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Slot Unavailable (409 Conflict)",
                            color = CrimsonRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = conflictError ?: "",
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
                        Text("Pick Slot", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Step Content Switcher
        Box(modifier = Modifier.weight(1f)) {
            when (step) {
                1 -> {
                    // STEP 1: Select Services
                    LazyColumn(
                        contentPadding = PaddingValues(20.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Text(
                                text = "Choose Service(s)",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "You can add multiple grooming treatments in a single appointment.",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        items(shop.services, key = { it.id }) { service ->
                            val isSelected = selectedServices.any { it.id == service.id }
                            Surface(
                                onClick = { viewModel.toggleServiceSelection(service) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) SurfaceElevated else SurfaceCard,
                                border = BorderStroke(1.dp, if (isSelected) AmberGold else BorderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { viewModel.toggleServiceSelection(service) },
                                        colors = CheckboxDefaults.colors(checkedColor = AmberGold, checkmarkColor = ObsidianDark)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(service.name, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("${service.durationMin} mins • ${service.category}", color = TextSecondary, fontSize = 12.sp)
                                    }
                                    Text("₹${service.price}", fontWeight = FontWeight.Bold, color = AmberGold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // STEP 2: Select Barber
                    LazyColumn(
                        contentPadding = PaddingValues(20.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Text(
                                text = "Choose Your Barber",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Choose 'Any Available Barber' for the quickest appointment or pick your favorite stylist.",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Any Available Barber
                            val isAny = selectedBarber == null
                            Surface(
                                onClick = { viewModel.selectBarber(null) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isAny) SurfaceElevated else SurfaceCard,
                                border = BorderStroke(1.dp, if (isAny) AmberGold else BorderSubtle),
                                modifier = Modifier.fillMaxWidth()
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
                                        Text("Best match auto-assigned at salon", color = TextSecondary, fontSize = 12.sp)
                                    }
                                    Surface(shape = RoundedCornerShape(6.dp), color = EmeraldGreenBg) {
                                        Text("Recommended", color = EmeraldGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
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
                                    .padding(vertical = 6.dp)
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
                3 -> {
                    // STEP 3: Date & Availability Slots
                    LazyColumn(
                        contentPadding = PaddingValues(20.dp),
                        modifier = Modifier.fillMaxSize()
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
                                text = "Real-time slots provided directly by salon database.",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Date strip
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                dates.forEach { d ->
                                    val isSelected = selectedDate == d
                                    Surface(
                                        onClick = { viewModel.selectDate(d) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) AmberGold else SurfaceCard,
                                        border = BorderStroke(1.dp, if (isSelected) AmberGoldLight else BorderSubtle),
                                        modifier = Modifier.width(100.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                                        ) {
                                            Text(
                                                text = d.substringBefore(" "),
                                                color = if (isSelected) ObsidianDark else TextPrimary,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = d.substringAfter(" ").take(7),
                                                color = if (isSelected) ObsidianDark else TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Testing Requirement 43: Simulate 409 Conflict Toggle
                            Surface(
                                shape = RoundedCornerShape(10.dp),
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
                                        Text(
                                            text = "Simulate 409 Booking Conflict",
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp)
                                ) {
                                    CircularProgressIndicator(color = AmberGold)
                                }
                            }
                        } else {
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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp)
                                    ) {
                                        slots.forEach { (slotTime, isAvailable) ->
                                            val isSelected = selectedSlot == slotTime
                                            Surface(
                                                onClick = {
                                                    if (isAvailable) {
                                                        viewModel.selectSlot(slotTime)
                                                    } else {
                                                        Toast.makeText(context, "Slot is already booked", Toast.LENGTH_SHORT).show()
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
                                                modifier = Modifier.width(96.dp)
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.padding(vertical = 10.dp)
                                                ) {
                                                    Text(
                                                        text = slotTime,
                                                        color = when {
                                                            !isAvailable -> TextTertiary
                                                            isSelected -> ObsidianDark
                                                            else -> TextPrimary
                                                        },
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        fontSize = 12.sp
                                                    )
                                                    Text(
                                                        text = if (isAvailable) "Available" else "Booked",
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
                4 -> {
                    // STEP 4: Review Booking Summary & Slot Hold
                    LazyColumn(
                        contentPadding = PaddingValues(20.dp),
                        modifier = Modifier.fillMaxSize()
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

                            // Temporary hold countdown timer badge
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

                                    // Selected Services
                                    Text("Services Selected:", color = TextSecondary, fontSize = 12.sp)
                                    selectedServices.forEach { s ->
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp)
                                        ) {
                                            Text("• ${s.name} (${s.durationMin}m)", color = TextPrimary, fontSize = 13.sp)
                                            Text("₹${s.price}", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = BorderSubtle)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Barber & Time
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Barber:", color = TextSecondary, fontSize = 13.sp)
                                        Text(selectedBarber?.name ?: "Any Available Barber", color = AmberGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Date & Time:", color = TextSecondary, fontSize = 13.sp)
                                        Text("$selectedDate @ $selectedSlot", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                                }
                            }
                        }
                    }
                }
                5 -> {
                    // STEP 5: Payment Gateway Simulation
                    LazyColumn(
                        contentPadding = PaddingValues(20.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Text(
                                text = "Select Payment Method",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Secure SSL 256-Bit Encrypted Payment Abstraction.",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Total to Pay Pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceElevated,
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Column {
                                        Text("Total Amount to Pay", color = TextSecondary, fontSize = 12.sp)
                                        Text("₹$grandTotal", fontWeight = FontWeight.ExtraBold, color = AmberGold, fontSize = 20.sp)
                                    }
                                    Surface(shape = RoundedCornerShape(8.dp), color = AmberGlow) {
                                        Text("Hold Active", color = AmberGold, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Payment Methods
                        val methods = listOf(
                            "UPI (Google Pay)" to Icons.Default.QrCodeScanner,
                            "UPI (PhonePe / Paytm)" to Icons.Default.AccountBalanceWallet,
                            "Credit / Debit Card" to Icons.Default.CreditCard,
                            "Net Banking" to Icons.Default.AccountBalance,
                            "Pay at Salon (Post-service)" to Icons.Default.Storefront
                        )

                        items(methods) { (method, icon) ->
                            val isSelected = selectedPaymentMethod == method
                            Surface(
                                onClick = { viewModel.selectedPaymentMethod.value = method },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) SurfaceElevated else SurfaceCard,
                                border = BorderStroke(1.dp, if (isSelected) AmberGold else BorderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.selectedPaymentMethod.value = method },
                                        colors = RadioButtonDefaults.colors(selectedColor = AmberGold)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Icon(imageVector = icon, contentDescription = null, tint = AmberGold, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = method, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
                6 -> {
                    // STEP 6: Booking Confirmed Screen
                    val booking = confirmedBooking
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldGreenBg,
                            border = BorderStroke(2.dp, EmeraldGreen),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Check, contentDescription = "Confirmed", tint = EmeraldGreen, modifier = Modifier.size(44.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

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

                        Spacer(modifier = Modifier.height(20.dp))

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Booking ID", color = TextSecondary, fontSize = 12.sp)
                                    Text(booking?.id ?: "BK10234", fontWeight = FontWeight.Bold, color = AmberGold, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Salon", color = TextSecondary, fontSize = 12.sp)
                                    Text(booking?.shopName ?: shop.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Barber", color = TextSecondary, fontSize = 12.sp)
                                    Text(booking?.barberName ?: "Master Barber", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Date & Time", color = TextSecondary, fontSize = 12.sp)
                                    Text("${booking?.dateStr ?: selectedDate} @ ${booking?.timeSlot ?: selectedSlot}", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Paid Amount", color = TextSecondary, fontSize = 12.sp)
                                    Text("₹${booking?.total ?: grandTotal}", fontWeight = FontWeight.ExtraBold, color = EmeraldGreen, fontSize = 14.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                viewModel.currentTab.value = 2 // My Bookings tab
                                viewModel.closeSubScreens()
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianDark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("View My Bookings", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                viewModel.currentTab.value = 0
                                viewModel.closeSubScreens()
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Back to Home", color = TextPrimary)
                        }
                    }
                }
            }
        }

        // Bottom CTA Bar for Steps 1 through 5
        if (step in 1..5) {
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
                            text = "Total Price",
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
                            when (step) {
                                1 -> viewModel.bookingStep.value = 2
                                2 -> viewModel.bookingStep.value = 3
                                3 -> viewModel.proceedToReview()
                                4 -> viewModel.proceedToPayment()
                                5 -> viewModel.completePaymentAndConfirm()
                            }
                        },
                        enabled = !isHoldingSlot && !isProcessingPayment && (step != 3 || selectedSlot != null),
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
                        if (isHoldingSlot || isProcessingPayment) {
                            CircularProgressIndicator(
                                color = ObsidianDark,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = when (step) {
                                    1 -> "Next: Choose Barber"
                                    2 -> "Next: Pick Time"
                                    3 -> "Reserve & Review"
                                    4 -> "Proceed to Payment"
                                    5 -> "Pay & Confirm Booking"
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
}
