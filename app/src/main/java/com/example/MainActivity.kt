package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.repository.BarberRepository
import com.example.ui.BarberViewModel
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getDatabase(applicationContext)
        val repository = BarberRepository(database = db)

        setContent {
            val viewModel: BarberViewModel = viewModel {
                BarberViewModel(repository)
            }

            BarberCraftTheme(darkTheme = true) {
                MainAppShell(viewModel)
            }
        }
    }
}

@Composable
fun MainAppShell(viewModel: BarberViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val upcomingBookings by viewModel.upcomingBookings.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadNotifsCount = notifications.count { !it.isRead }

    val showFilterSheet by viewModel.showFilterSheet.collectAsState()
    val showLocationPicker by viewModel.showLocationPicker.collectAsState()
    val cancelTarget by viewModel.cancelModalBooking.collectAsState()
    val rescheduleTarget by viewModel.rescheduleModalBooking.collectAsState()
    val reviewTarget by viewModel.reviewModalBooking.collectAsState()
    val directionsShop by viewModel.showDirectionsShop.collectAsState()
    val city by viewModel.selectedCity.collectAsState()
    val isDetectingGps by viewModel.isDetectingLocation.collectAsState()
    val filterCriteria by viewModel.filterCriteria.collectAsState()

    val context = LocalContext.current

    if (!isLoggedIn) {
        AuthScreen(viewModel = viewModel)
    } else {
        Scaffold(
            bottomBar = {
                // Only show bottom navigation when on the main tabs, not inside detail/booking flow
                if (currentScreen == null) {
                    NavigationBar(
                        containerColor = SurfaceDark,
                        tonalElevation = 12.dp,
                        modifier = Modifier.testTag("main_bottom_nav")
                    ) {
                        // Home Tab
                        NavigationBarItem(
                            selected = currentTab == 0,
                            onClick = { viewModel.currentTab.value = 0 },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == 0) Icons.Default.Home else Icons.Outlined.Home,
                                    contentDescription = "Home"
                                )
                            },
                            label = { Text("Home", fontSize = 11.sp, fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ObsidianDark,
                                selectedTextColor = AmberGold,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = AmberGold
                            ),
                            modifier = Modifier.testTag("nav_tab_home")
                        )

                        // Explore Tab
                        NavigationBarItem(
                            selected = currentTab == 1,
                            onClick = { viewModel.currentTab.value = 1 },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == 1) Icons.Default.Search else Icons.Outlined.Search,
                                    contentDescription = "Explore"
                                )
                            },
                            label = { Text("Explore", fontSize = 11.sp, fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ObsidianDark,
                                selectedTextColor = AmberGold,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = AmberGold
                            ),
                            modifier = Modifier.testTag("nav_tab_explore")
                        )

                        // Bookings Tab
                        NavigationBarItem(
                            selected = currentTab == 2,
                            onClick = { viewModel.currentTab.value = 2 },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (upcomingBookings.isNotEmpty()) {
                                            Badge(containerColor = AmberGold, contentColor = ObsidianDark) {
                                                Text("${upcomingBookings.size}")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (currentTab == 2) Icons.Default.CalendarToday else Icons.Outlined.CalendarToday,
                                        contentDescription = "Bookings"
                                    )
                                }
                            },
                            label = { Text("Bookings", fontSize = 11.sp, fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ObsidianDark,
                                selectedTextColor = AmberGold,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = AmberGold
                            ),
                            modifier = Modifier.testTag("nav_tab_bookings")
                        )

                        // Notifications Tab
                        NavigationBarItem(
                            selected = currentTab == 3,
                            onClick = { viewModel.currentTab.value = 3 },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (unreadNotifsCount > 0) {
                                            Badge(containerColor = CrimsonRed, contentColor = Color.White) {
                                                Text("$unreadNotifsCount")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (currentTab == 3) Icons.Default.Notifications else Icons.Outlined.Notifications,
                                        contentDescription = "Notifications"
                                    )
                                }
                            },
                            label = { Text("Alerts", fontSize = 11.sp, fontWeight = if (currentTab == 3) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ObsidianDark,
                                selectedTextColor = AmberGold,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = AmberGold
                            ),
                            modifier = Modifier.testTag("nav_tab_notifications")
                        )

                        // Profile Tab
                        NavigationBarItem(
                            selected = currentTab == 4,
                            onClick = { viewModel.currentTab.value = 4 },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == 4) Icons.Default.Person else Icons.Outlined.Person,
                                    contentDescription = "Profile"
                                )
                            },
                            label = { Text("Profile", fontSize = 11.sp, fontWeight = if (currentTab == 4) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ObsidianDark,
                                selectedTextColor = AmberGold,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = AmberGold
                            ),
                            modifier = Modifier.testTag("nav_tab_profile")
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (currentScreen == null) innerPadding else PaddingValues(0.dp))
            ) {
                // Screen routing
                when (currentScreen) {
                    "SHOP_DETAIL" -> {
                        ShopDetailScreen(viewModel = viewModel)
                    }
                    "BOOKING_FLOW" -> {
                        BookingFlowScreen(viewModel = viewModel)
                    }
                    else -> {
                        // Main Bottom Tabs
                        when (currentTab) {
                            0 -> HomeScreen(viewModel = viewModel)
                            1 -> ExploreScreen(viewModel = viewModel)
                            2 -> BookingsTabScreen(viewModel = viewModel)
                            3 -> NotificationsTabScreen(viewModel = viewModel)
                            4 -> ProfileTabScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }

        // Overlay Dialogs & Sheets
        if (showFilterSheet) {
            FilterBottomSheet(
                currentFilter = filterCriteria,
                onDismiss = { viewModel.showFilterSheet.value = false },
                onApply = { newCriteria ->
                    viewModel.filterCriteria.value = newCriteria
                }
            )
        }

        if (showLocationPicker) {
            LocationPickerModal(
                currentCity = city,
                onSelectCity = { selected ->
                    viewModel.selectedCity.value = selected
                },
                onDetectGps = {
                    viewModel.detectLocationSimulated()
                },
                isDetecting = isDetectingGps,
                onDismiss = { viewModel.showLocationPicker.value = false }
            )
        }

        cancelTarget?.let { booking ->
            CancelBookingDialog(
                booking = booking,
                onConfirmCancel = {
                    viewModel.cancelActiveBooking(booking.id)
                    Toast.makeText(context, "Appointment cancelled & refund initiated.", Toast.LENGTH_LONG).show()
                },
                onDismiss = { viewModel.cancelModalBooking.value = null }
            )
        }

        rescheduleTarget?.let { booking ->
            RescheduleDialog(
                booking = booking,
                onConfirmReschedule = { newDate, newTime ->
                    viewModel.rescheduleNewDate.value = newDate
                    viewModel.rescheduleNewTime.value = newTime
                    viewModel.rescheduleActiveBooking(booking.id)
                    Toast.makeText(context, "Appointment rescheduled to $newDate at $newTime.", Toast.LENGTH_LONG).show()
                },
                onDismiss = { viewModel.rescheduleModalBooking.value = null }
            )
        }

        reviewTarget?.let { booking ->
            ReviewDialog(
                booking = booking,
                onSubmitReview = { rating, comment ->
                    Toast.makeText(context, "Thank you! Your $rating★ review has been posted.", Toast.LENGTH_LONG).show()
                    viewModel.reviewModalBooking.value = null
                },
                onDismiss = { viewModel.reviewModalBooking.value = null }
            )
        }

        directionsShop?.let { shop ->
            DirectionsDialog(
                shop = shop,
                onDismiss = { viewModel.showDirectionsShop.value = null }
            )
        }
    }
}
