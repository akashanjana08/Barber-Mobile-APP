package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.BarberViewModel
import com.example.ui.theme.*

@Composable
fun AuthScreen(
    viewModel: BarberViewModel,
    modifier: Modifier = Modifier
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("Akash Sharma") }
    var phoneOrEmail by remember { mutableStateOf("+91 98765 12340") }
    var password by remember { mutableStateOf("••••••••") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
            .testTag("auth_screen")
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // App Branding Logo
        Surface(
            shape = CircleShape,
            color = SurfaceElevated,
            border = BorderStroke(2.dp, AmberGold),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = "BarberCraft",
                    tint = AmberGold,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "BARBERCRAFT",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                color = AmberGold
            )
        )
        Text(
            text = "Luxury Barber & Salon Appointments",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Toggle Login vs Register
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceCard)
                .padding(4.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (!isRegisterMode) AmberGold else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable {
                        isRegisterMode = false
                        errorMessage = null
                    }
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    text = "Sign In",
                    fontWeight = FontWeight.Bold,
                    color = if (!isRegisterMode) ObsidianDark else TextSecondary,
                    fontSize = 14.sp
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isRegisterMode) AmberGold else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable {
                        isRegisterMode = true
                        errorMessage = null
                    }
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    text = "Create Account",
                    fontWeight = FontWeight.Bold,
                    color = if (isRegisterMode) ObsidianDark else TextSecondary,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Form fields
        if (isRegisterMode) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = AmberGold) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AmberGold,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = phoneOrEmail,
            onValueChange = { phoneOrEmail = it },
            label = { Text(if (isRegisterMode) "Mobile Number / Email" else "Mobile or Email") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = AmberGold) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AmberGold,
                unfocusedBorderColor = BorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = AmberGold) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Password",
                        tint = TextSecondary
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AmberGold,
                unfocusedBorderColor = BorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorMessage ?: "", color = CrimsonRed, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (phoneOrEmail.isBlank()) {
                    errorMessage = "Please enter your mobile or email"
                } else {
                    if (isRegisterMode && name.isNotBlank()) {
                        viewModel.updateProfile(name, phoneOrEmail, "+91 98765 12340")
                    }
                    viewModel.isLoggedIn.value = true
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AmberGold,
                contentColor = ObsidianDark
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("auth_submit_button")
        ) {
            Text(
                text = if (isRegisterMode) "Register & Start Booking" else "Sign In",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Demo Fast Login Button
        OutlinedButton(
            onClick = {
                viewModel.isLoggedIn.value = true
            },
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(Icons.Default.FlashOn, contentDescription = null, tint = AmberGold)
            Spacer(modifier = Modifier.width(8.dp))
            Text("One-Tap Demo Login (Akash Sharma)", color = TextPrimary, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(30.dp))
        Text(
            text = "By signing in, you agree to BarberCraft Terms of Service & Privacy Policy.",
            color = TextTertiary,
            fontSize = 11.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
