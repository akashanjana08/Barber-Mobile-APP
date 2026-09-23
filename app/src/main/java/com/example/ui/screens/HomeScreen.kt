package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import com.example.ui.components.BarberShopCard
import com.example.ui.components.CategoryChip
import com.example.ui.components.LocationPill
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: BarberViewModel,
    modifier: Modifier = Modifier
) {
    val user by viewModel.userProfile.collectAsState()
    val city by viewModel.selectedCity.collectAsState()
    val shops by viewModel.filteredShops.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    var searchInput by remember { mutableStateOf("") }

    val categories = listOf("Haircut", "Beard", "Hair Color", "Facial", "Hair Spa", "Kids Haircut")

    LazyColumn(
        contentPadding = PaddingValues(bottom = 90.dp),
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .testTag("home_screen_content")
    ) {
        // Top Header: User Greeting & Location
        item {
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
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Good Morning, ${user.name.split(" ").first()} 👋",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LocationPill(
                            cityName = city,
                            onClick = { viewModel.showLocationPicker.value = true }
                        )
                    }

                    // Avatar button
                    Surface(
                        onClick = { viewModel.currentTab.value = 4 },
                        shape = CircleShape,
                        border = BorderStroke(2.dp, AmberGold),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_avatar_akash),
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Search Bar with Filter Button
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                OutlinedTextField(
                    value = searchInput,
                    onValueChange = {
                        searchInput = it
                        viewModel.searchQuery.value = it
                    },
                    placeholder = {
                        Text(
                            "Search barber, shop or service...",
                            color = TextTertiary,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = AmberGold
                        )
                    },
                    trailingIcon = {
                        if (searchInput.isNotEmpty()) {
                            IconButton(onClick = {
                                searchInput = ""
                                viewModel.searchQuery.value = ""
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard,
                        focusedBorderColor = AmberGold,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_search_input")
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Filter button
                Surface(
                    onClick = { viewModel.showFilterSheet.value = true },
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("home_filter_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filters",
                            tint = AmberGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Promotional Hero Card Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
                    .clickable {
                        // Filter for royal haircut promotion
                        viewModel.selectedCategory.value = "Haircut"
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner),
                        contentDescription = "Promo Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xF00F0E11),
                                        Color(0xCC0F0E11),
                                        Color(0x400F0E11)
                                    )
                                )
                            )
                    )

                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 18.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AmberGold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = "EXCLUSIVE 20% OFF",
                                color = ObsidianDark,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "Premium Master Cut & Shave",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )

                        Text(
                            text = "Book with certified master stylists in Noida",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }
            }
        }

        // Category Chips Horizontal Scroll
        item {
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

                    if (selectedCategory != null) {
                        Text(
                            text = "Clear Filter",
                            color = AmberGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { viewModel.selectedCategory.value = null }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    categories.forEach { cat ->
                        CategoryChip(
                            name = cat,
                            isSelected = selectedCategory == cat,
                            onClick = {
                                viewModel.selectedCategory.value =
                                    if (selectedCategory == cat) null else cat
                            }
                        )
                    }
                }
            }
        }

        // Nearby Barbers Section Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Column {
                    Text(
                        text = "Nearby Barbers",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "${shops.size} shops ready for appointments",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                Surface(
                    onClick = { viewModel.showFilterSheet.value = true },
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceElevated
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort",
                            tint = AmberGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Sort",
                            fontSize = 12.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Shops List Items
        if (shops.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCut,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No barbers found nearby",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try increasing your search radius or changing filters.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.searchQuery.value = ""
                                viewModel.selectedCategory.value = null
                                viewModel.filterCriteria.value = FilterCriteria()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberGold,
                                contentColor = ObsidianDark
                            )
                        ) {
                            Text("Reset All Filters", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(shops, key = { it.id }) { shop ->
                BarberShopCard(
                    shop = shop,
                    onClick = { viewModel.openShopDetail(shop) },
                    onFavoriteToggle = { viewModel.toggleFavorite(shop.id) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }
    }
}
