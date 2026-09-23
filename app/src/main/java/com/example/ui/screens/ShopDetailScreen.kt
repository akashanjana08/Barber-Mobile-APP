package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.*
import com.example.ui.BarberViewModel
import com.example.ui.theme.*

@Composable
fun ShopDetailScreen(
    viewModel: BarberViewModel,
    modifier: Modifier = Modifier
) {
    val shop = viewModel.selectedShop.collectAsState().value ?: return
    val selectedServices by viewModel.selectedServices.collectAsState()
    val selectedBarber by viewModel.selectedBarber.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Services, 1: Barbers, 2: Reviews, 3: About

    val totalPrice = selectedServices.sumOf { it.price }
    val totalDuration = selectedServices.sumOf { it.durationMin }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .testTag("shop_detail_screen")
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 120.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Hero Photo Header with back button & favorite heart
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    Image(
                        painter = painterResource(id = shop.imageDrawableRes),
                        contentDescription = shop.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x99000000),
                                        Color.Transparent,
                                        Color(0xE60F0E11)
                                    )
                                )
                            )
                    )

                    // Top Action Bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.closeSubScreens() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SurfaceDark.copy(alpha = 0.85f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }

                        Row {
                            IconButton(
                                onClick = { viewModel.toggleFavorite(shop.id) },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(SurfaceDark.copy(alpha = 0.85f))
                            ) {
                                Icon(
                                    imageVector = if (shop.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (shop.isFavorite) CrimsonRed else TextPrimary
                                )
                            }
                        }
                    }

                    // Bottom info on hero image
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (shop.isOpen) EmeraldGreenBg else CrimsonRedBg,
                            border = BorderStroke(1.dp, if (shop.isOpen) EmeraldGreen else CrimsonRed)
                        ) {
                            Text(
                                text = if (shop.isOpen) "● Open Today" else "● Closed Now",
                                color = if (shop.isOpen) EmeraldGreen else CrimsonRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = shop.name,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                }
            }

            // Shop summary row: Rating, Distance, Price
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = StarYellow, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${shop.rating}", fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Text("${shop.reviewCount} reviews", fontSize = 11.sp, color = TextSecondary)
                        }

                        VerticalDivider(modifier = Modifier.height(30.dp), color = BorderSubtle)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Navigation, contentDescription = null, tint = SkyBlue, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${shop.distanceKm} km", fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Text("Distance", fontSize = 11.sp, color = TextSecondary)
                        }

                        VerticalDivider(modifier = Modifier.height(30.dp), color = BorderSubtle)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("₹${shop.startingPrice}+", fontWeight = FontWeight.Bold, color = AmberGold)
                            Text("Starting price", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }

            // Address & Get Directions Card
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = AmberGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = shop.address,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedButton(
                            onClick = { viewModel.showDirectionsShop.value = shop },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Directions, contentDescription = null, tint = AmberGold, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Directions", color = AmberGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Tab bar: Services, Barbers, Reviews, About
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .padding(4.dp)
                ) {
                    listOf("Services", "Barbers", "Reviews", "About").forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) AmberGold else Color.Transparent)
                                .clickable { selectedTab = index }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) ObsidianDark else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // Services List
                    items(shop.services, key = { it.id }) { service ->
                        val isSelected = selectedServices.any { it.id == service.id }
                        Surface(
                            onClick = { viewModel.toggleServiceSelection(service) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) SurfaceElevated else SurfaceCard,
                            border = BorderStroke(1.dp, if (isSelected) AmberGold else BorderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                                .testTag("service_item_${service.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { viewModel.toggleServiceSelection(service) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = AmberGold,
                                        checkmarkColor = ObsidianDark
                                    )
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = service.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${service.durationMin} mins • ${service.category}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                    )
                                    if (service.description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = service.description,
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary),
                                            fontSize = 12.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Text(
                                    text = "₹${service.price}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = AmberGold
                                    )
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Barbers Selection Tab
                    item {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Text(
                                text = "Select Your Barber",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Choose a master barber or let us assign the best available one for your slot.",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            // "Any Available Barber" Option
                            val isAnySelected = selectedBarber == null
                            Surface(
                                onClick = { viewModel.selectBarber(null) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isAnySelected) SurfaceElevated else SurfaceCard,
                                border = BorderStroke(1.dp, if (isAnySelected) AmberGold else BorderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    RadioButton(
                                        selected = isAnySelected,
                                        onClick = { viewModel.selectBarber(null) },
                                        colors = RadioButtonDefaults.colors(selectedColor = AmberGold)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Any Available Barber",
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "Fastest availability • Auto-assigned on booking",
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = EmeraldGreenBg
                                    ) {
                                        Text(
                                            text = "Instant",
                                            color = EmeraldGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
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
                                .padding(horizontal = 20.dp, vertical = 6.dp)
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

                                Surface(
                                    shape = CircleShape,
                                    color = SurfaceElevated,
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = barber.name,
                                            tint = AmberGold,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = barber.name,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "${barber.specialty} • ${barber.experienceYears} yrs exp",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
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
                                    Text("${barber.rating}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Reviews Tab
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceAround,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${shop.rating}",
                                        style = MaterialTheme.typography.displaySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = AmberGold
                                        )
                                    )
                                    Row {
                                        repeat(5) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = StarYellow, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Text(
                                        text = "Based on ${shop.reviewCount} reviews",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    items(shop.reviews, key = { it.id }) { review ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = review.userName,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = review.date,
                                        color = TextTertiary,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    repeat(review.rating.toInt()) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = StarYellow, modifier = Modifier.size(13.dp))
                                    }
                                    if (review.barberName.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "• Stylist: ${review.barberName}",
                                            color = AmberGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "\"${review.comment}\"",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
                3 -> {
                    // About Tab
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "About the Salon",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = shop.description,
                                    color = TextSecondary,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Amenities & Highlights",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                listOf(
                                    "☕ Complimentary craft espresso & refreshments",
                                    "📶 High-speed guest Wi-Fi",
                                    "🧴 Organic, premium grooming balms & imported oils",
                                    "❄️ Fully climate-controlled lounge",
                                    "🚗 Reserved valet & mall parking available"
                                ).forEach { amenity ->
                                    Text(
                                        text = amenity,
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sticky Bottom Booking Bar
        Surface(
            color = SurfaceDark,
            tonalElevation = 16.dp,
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Column {
                    Text(
                        text = "${selectedServices.size} Service(s) selected",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "₹$totalPrice",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = AmberGold
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• $totalDuration mins",
                            color = TextTertiary,
                            fontSize = 12.sp
                        )
                    }
                }

                Button(
                    onClick = { viewModel.startBookingFlow() },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberGold,
                        contentColor = ObsidianDark
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("proceed_booking_button")
                ) {
                    Text(
                        text = "Select Time Slot",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
