package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.data.model.SortOption
import com.example.ui.BarberViewModel
import com.example.ui.components.BarberShopCard
import com.example.ui.theme.*

@Composable
fun ExploreScreen(
    viewModel: BarberViewModel,
    modifier: Modifier = Modifier
) {
    val shops by viewModel.filteredShops.collectAsState()
    val filter by viewModel.filterCriteria.collectAsState()
    var searchInput by remember { mutableStateOf(viewModel.searchQuery.value) }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 90.dp),
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .testTag("explore_screen")
    ) {
        // Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Explore & Discover",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "Find premium barbers, styling lounges & specialist cuts",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Search field
                OutlinedTextField(
                    value = searchInput,
                    onValueChange = {
                        searchInput = it
                        viewModel.searchQuery.value = it
                    },
                    placeholder = {
                        Text("Search shop, stylist, beard trim or haircut...", color = TextTertiary, fontSize = 14.sp)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = AmberGold)
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
                        .fillMaxWidth()
                        .testTag("explore_search_field")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Quick filter chips row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    // Filter Sheet trigger button
                    Surface(
                        onClick = { viewModel.showFilterSheet.value = true },
                        shape = RoundedCornerShape(12.dp),
                        color = AmberGold,
                        contentColor = ObsidianDark
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("All Filters", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Open Now toggle
                    val openOnly = filter.openNowOnly
                    Surface(
                        onClick = {
                            viewModel.filterCriteria.value = filter.copy(openNowOnly = !openOnly)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (openOnly) EmeraldGreenBg else SurfaceElevated,
                        border = BorderStroke(1.dp, if (openOnly) EmeraldGreen else BorderSubtle)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "Open Now",
                                color = if (openOnly) EmeraldGreen else TextPrimary,
                                fontWeight = if (openOnly) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Top rated chip
                    val isTopRated = filter.minRating >= 4.5
                    Surface(
                        onClick = {
                            viewModel.filterCriteria.value = filter.copy(minRating = if (isTopRated) 0.0 else 4.5)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isTopRated) AmberGlow else SurfaceElevated,
                        border = BorderStroke(1.dp, if (isTopRated) AmberGold else BorderSubtle)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = StarYellow, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "4.5+ Rating",
                                color = if (isTopRated) AmberGold else TextPrimary,
                                fontWeight = if (isTopRated) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Nearby chip (< 3km)
                    val isNearby = filter.maxDistanceKm <= 3.0
                    Surface(
                        onClick = {
                            viewModel.filterCriteria.value = filter.copy(maxDistanceKm = if (isNearby) 10.0 else 3.0)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isNearby) SkyBlueBg else SurfaceElevated,
                        border = BorderStroke(1.dp, if (isNearby) SkyBlue else BorderSubtle)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "Under 3 km",
                                color = if (isNearby) SkyBlue else TextPrimary,
                                fontWeight = if (isNearby) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Results count
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Showing ${shops.size} results",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                val sortLabel = when (filter.sortBy) {
                    SortOption.RECOMMENDED -> "Recommended"
                    SortOption.NEAREST -> "Nearest"
                    SortOption.HIGHEST_RATED -> "Highest Rated"
                    SortOption.LOWEST_PRICE -> "Lowest Price"
                }

                Text(
                    text = "Sorted by: $sortLabel",
                    fontSize = 11.sp,
                    color = AmberGold,
                    modifier = Modifier.clickable { viewModel.showFilterSheet.value = true }
                )
            }
        }

        // Shop items
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
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No salons match your search",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try clearing filters or searching for 'Haircut', 'Royal', or 'Fade'.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
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
