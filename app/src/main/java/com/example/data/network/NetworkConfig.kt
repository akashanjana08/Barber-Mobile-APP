package com.example.data.network

import android.content.Context
import android.content.SharedPreferences

object NetworkConfig {
    const val PREFS_NAME = "barbercraft_network_prefs"
    const val KEY_BASE_URL = "custom_base_url"

    // Primary Production WAN Public Domain
    const val DEFAULT_PUBLIC_BASE_URL = "https://jkwl8mtg-5000.inc1.devtunnels.ms/api/v1/"

    // Local Emulator Fallback
    const val LOCAL_EMULATOR_BASE_URL = "http://10.0.2.2:5000/api/v1/"

    // Local LAN / WiFi Fallback
    const val LOCAL_WIFI_BASE_URL = "http://192.168.1.100:5000/api/v1/"

    @Volatile
    private var activeBaseUrl: String = DEFAULT_PUBLIC_BASE_URL

    fun getBaseUrl(): String = activeBaseUrl

    fun setBaseUrl(url: String, context: Context? = null) {
        val sanitized = if (!url.endsWith("/")) "$url/" else url
        activeBaseUrl = sanitized
        context?.let {
            val prefs = it.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_BASE_URL, sanitized).apply()
        }
    }

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_BASE_URL, null)
        if (!saved.isNullOrBlank()) {
            activeBaseUrl = saved
        }
    }
}

enum class ApiConnectionState(val label: String, val colorHex: Long) {
    CONNECTED("Cloud API Live", 0xFF10B981),      // Green
    CONNECTING("Connecting...", 0xFFF59E0B),       // Amber
    OFFLINE("Offline Cache", 0xFFEF4444)          // Red
}

data class ApiHealthStatus(
    val state: ApiConnectionState = ApiConnectionState.CONNECTING,
    val endpointUrl: String = NetworkConfig.DEFAULT_PUBLIC_BASE_URL,
    val latencyMs: Long = 0,
    val serviceName: String = "BarberCraft Express API",
    val isDatabaseConnected: Boolean = true,
    val lastPingTimestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)
