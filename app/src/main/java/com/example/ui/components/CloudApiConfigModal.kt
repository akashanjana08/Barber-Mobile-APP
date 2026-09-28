package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.network.ApiConnectionState
import com.example.data.network.ApiHealthStatus
import com.example.data.network.NetworkConfig
import com.example.ui.theme.*

@Composable
fun CloudApiChip(
    apiHealth: ApiHealthStatus,
    isSyncing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (apiHealth.state) {
        ApiConnectionState.CONNECTED -> EmeraldGreen
        ApiConnectionState.CONNECTING -> AmberGold
        ApiConnectionState.OFFLINE -> CrimsonRed
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SurfaceElevated.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag("cloud_api_chip")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(statusColor, CircleShape)
            )

            Text(
                text = if (isSyncing) "Syncing..." else apiHealth.state.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            if (apiHealth.state == ApiConnectionState.CONNECTED && apiHealth.latencyMs > 0) {
                Text(
                    text = "${apiHealth.latencyMs}ms",
                    fontSize = 10.sp,
                    color = AmberGoldLight,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun CloudApiConfigDialog(
    apiHealth: ApiHealthStatus,
    isSyncing: Boolean,
    onDismiss: () -> Unit,
    onRefreshSync: () -> Unit,
    onUpdateUrl: (String) -> Unit
) {
    var customUrlInput by remember { mutableStateOf(apiHealth.endpointUrl) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("cloud_api_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Cloud API",
                            tint = AmberGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Cloud Production API",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                // Connection Card
                val statusColor = when (apiHealth.state) {
                    ApiConnectionState.CONNECTED -> EmeraldGreen
                    ApiConnectionState.CONNECTING -> AmberGold
                    ApiConnectionState.OFFLINE -> CrimsonRed
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(statusColor, CircleShape)
                            )
                            Text(
                                text = "Status: ${apiHealth.state.label}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        if (apiHealth.state == ApiConnectionState.CONNECTED) {
                            Text(
                                text = "• Service: ${apiHealth.serviceName}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "• Database: MongoDB Engine Active",
                                fontSize = 12.sp,
                                color = EmeraldGreen
                            )
                            Text(
                                text = "• Latency: ${apiHealth.latencyMs} ms round-trip",
                                fontSize = 12.sp,
                                color = AmberGoldLight
                            )
                        } else if (apiHealth.errorMessage != null) {
                            Text(
                                text = "Notice: ${apiHealth.errorMessage}",
                                fontSize = 12.sp,
                                color = CrimsonRed
                            )
                        }
                    }
                }

                // Current Active Endpoint
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Active Base URL (WAN / Internet):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceCard,
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = apiHealth.endpointUrl,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = AmberGoldLight,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Preset Domain Selectors
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Quick Presets:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                customUrlInput = NetworkConfig.DEFAULT_PUBLIC_BASE_URL
                                onUpdateUrl(NetworkConfig.DEFAULT_PUBLIC_BASE_URL)
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "WAN Tunnel",
                                fontSize = 11.sp,
                                color = AmberGold,
                                maxLines = 1
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                customUrlInput = NetworkConfig.LOCAL_EMULATOR_BASE_URL
                                onUpdateUrl(NetworkConfig.LOCAL_EMULATOR_BASE_URL)
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Emulator",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Custom URL Input
                OutlinedTextField(
                    value = customUrlInput,
                    onValueChange = { customUrlInput = it },
                    label = { Text("Custom API Endpoint", fontSize = 12.sp) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = TextPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberGold,
                        unfocusedBorderColor = BorderSubtle,
                        focusedLabelColor = AmberGold,
                        unfocusedLabelColor = TextTertiary,
                        cursorColor = AmberGold
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("custom_api_input")
                )

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onUpdateUrl(customUrlInput) },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AmberGold),
                        modifier = Modifier.weight(1f).testTag("save_api_url_btn")
                    ) {
                        Text("Apply URL", color = AmberGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onRefreshSync,
                        enabled = !isSyncing,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                        modifier = Modifier.weight(1f).testTag("sync_api_btn")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                color = ObsidianDark,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text("Test Ping", color = ObsidianDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
