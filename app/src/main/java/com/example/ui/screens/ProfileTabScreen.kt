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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.BarberViewModel
import com.example.ui.theme.*

@Composable
fun ProfileTabScreen(
    viewModel: BarberViewModel,
    modifier: Modifier = Modifier
) {
    val user by viewModel.userProfile.collectAsState()
    val shops by viewModel.allShops.collectAsState()
    val favoriteShops = shops.filter { it.isFavorite }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showPolicyDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .testTag("profile_tab_screen")
    ) {
        // Header
        item {
            Column(modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)) {
                Text(
                    text = "Customer Profile",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "Personal details, preferences & saved barbers",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }

        // Profile Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(18.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        border = BorderStroke(2.dp, AmberGold),
                        modifier = Modifier.size(70.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_avatar_akash),
                            contentDescription = "Profile Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                        Text(
                            text = user.phone,
                            style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary)
                        )
                    }

                    IconButton(
                        onClick = { showEditProfileDialog = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = AmberGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Saved Barbers Section
        if (favoriteShops.isNotEmpty()) {
            item {
                Text(
                    text = "Saved Barber Shops (${favoriteShops.size})",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(favoriteShops, key = { it.id }) { shop ->
                Surface(
                    onClick = { viewModel.openShopDetail(shop) },
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Image(
                                painter = painterResource(id = shop.imageDrawableRes),
                                contentDescription = shop.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(shop.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                            Text("⭐ ${shop.rating} • ${shop.distanceKm} km away", color = TextSecondary, fontSize = 12.sp)
                        }

                        IconButton(onClick = { viewModel.toggleFavorite(shop.id) }) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = CrimsonRed)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // Account Options Menu
        item {
            Text(
                text = "Preferences & Support",
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ProfileMenuItem(
                        icon = Icons.Default.CalendarToday,
                        title = "Booking History",
                        subtitle = "View past salon visits & invoices",
                        onClick = { viewModel.currentTab.value = 2 }
                    )
                    HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(horizontal = 16.dp))

                    ProfileMenuItem(
                        icon = Icons.Default.CreditCard,
                        title = "Payment Methods",
                        subtitle = "Saved UPI IDs, cards & net banking",
                        onClick = { /* Informational */ }
                    )
                    HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(horizontal = 16.dp))

                    ProfileMenuItem(
                        icon = Icons.Default.Policy,
                        title = "Cancellation Policy",
                        subtitle = "Free 2-hour cancellation rules",
                        onClick = { showPolicyDialog = true }
                    )
                    HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(horizontal = 16.dp))

                    ProfileMenuItem(
                        icon = Icons.Default.HelpOutline,
                        title = "Help & Support FAQ",
                        subtitle = "Instant customer concierge assistance",
                        onClick = { showHelpDialog = true }
                    )
                    HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(horizontal = 16.dp))

                    ProfileMenuItem(
                        icon = Icons.Default.Logout,
                        title = "Log Out",
                        subtitle = "Sign out of your customer account",
                        textColor = CrimsonRed,
                        onClick = { viewModel.isLoggedIn.value = false }
                    )
                }
            }
        }
    }

    // Edit Profile Modal
    if (showEditProfileDialog) {
        var nameInput by remember { mutableStateOf(user.name) }
        var emailInput by remember { mutableStateOf(user.email) }
        var phoneInput by remember { mutableStateOf(user.phone) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = SurfaceDark,
            title = { Text("Edit Profile", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Full Name") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AmberGold, unfocusedBorderColor = BorderSubtle)
                    )
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Address") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AmberGold, unfocusedBorderColor = BorderSubtle)
                    )
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Mobile Number") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AmberGold, unfocusedBorderColor = BorderSubtle)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProfile(nameInput, emailInput, phoneInput)
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianDark)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Cancellation Policy Dialog
    if (showPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPolicyDialog = false },
            containerColor = SurfaceDark,
            title = { Text("Cancellation & Rescheduling", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Free Cancellation: You can cancel free of charge up to 2 hours before the start time.", color = TextSecondary, fontSize = 13.sp)
                    Text("• Instant Refunds: All pre-paid UPI and card transactions are reversed within 24-48 hours.", color = TextSecondary, fontSize = 13.sp)
                    Text("• Easy Rescheduling: You can move your slot to any available date and time with zero penalty.", color = TextSecondary, fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(onClick = { showPolicyDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianDark)) {
                    Text("Understood", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Help FAQ Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            containerColor = SurfaceDark,
            title = { Text("Help & Support Concierge", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Q: How does salon slot reservation work?", fontWeight = FontWeight.Bold, color = AmberGold, fontSize = 13.sp)
                    Text("A: When you pick a slot, our backend temporarily locks it for 10 minutes so no other client can book it while you pay.", color = TextSecondary, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Q: Can I pay directly at the shop?", fontWeight = FontWeight.Bold, color = AmberGold, fontSize = 13.sp)
                    Text("A: Yes! Select 'Pay at Salon' during the checkout step.", color = TextSecondary, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Contact support:", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                    Text("support@barbercraft.app • +91 80000 12345", color = AmberGold, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(onClick = { showHelpDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianDark)) {
                    Text("Got It", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    textColor: androidx.compose.ui.graphics.Color = TextPrimary,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = SurfaceElevated,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (textColor != TextPrimary) textColor else AmberGold,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.sp)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }

        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextTertiary)
    }
}
