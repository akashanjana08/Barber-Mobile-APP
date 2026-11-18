package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SessionManager {
    private const val PREFS_NAME = "barbercraft_auth_prefs"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_PHONE = "user_phone"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    private var prefs: SharedPreferences? = null

    private val _isLoggedInState = MutableStateFlow(false)
    val isLoggedInState: StateFlow<Boolean> = _isLoggedInState.asStateFlow()

    private val _currentUserState = MutableStateFlow(
        User(
            id = "",
            name = "Guest User",
            email = "",
            phone = "",
            token = "",
            refreshToken = ""
        )
    )
    val currentUserState: StateFlow<User> = _currentUserState.asStateFlow()

    @Volatile
    var authToken: String? = null
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs?.let { p ->
            val loggedIn = p.getBoolean(KEY_IS_LOGGED_IN, false)
            authToken = p.getString(KEY_AUTH_TOKEN, null)

            val id = p.getString(KEY_USER_ID, "") ?: ""
            val name = p.getString(KEY_USER_NAME, "Guest User") ?: "Guest User"
            val email = p.getString(KEY_USER_EMAIL, "") ?: ""
            val phone = p.getString(KEY_USER_PHONE, "") ?: ""
            val refresh = p.getString(KEY_REFRESH_TOKEN, "") ?: ""

            if (loggedIn && id.isNotBlank()) {
                _currentUserState.value = User(
                    id = id,
                    name = name,
                    email = email,
                    phone = phone,
                    token = authToken ?: "",
                    refreshToken = refresh
                )
                _isLoggedInState.value = true
            } else {
                _currentUserState.value = User(
                    id = "",
                    name = "Guest User",
                    email = "",
                    phone = "",
                    token = "",
                    refreshToken = ""
                )
                _isLoggedInState.value = false
            }
        }
    }

    fun saveAuthSession(user: User, token: String?, refreshToken: String?) {
        authToken = token
        _currentUserState.value = user.copy(
            token = token ?: user.token,
            refreshToken = refreshToken ?: user.refreshToken
        )
        _isLoggedInState.value = true

        prefs?.edit()?.apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_AUTH_TOKEN, token)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putString(KEY_USER_ID, user.id)
            putString(KEY_USER_NAME, user.name)
            putString(KEY_USER_EMAIL, user.email)
            putString(KEY_USER_PHONE, user.phone)
            apply()
        }
    }

    fun clearSession() {
        authToken = null
        _isLoggedInState.value = false
        _currentUserState.value = User(
            id = "",
            name = "Guest User",
            email = "",
            phone = "",
            token = "",
            refreshToken = ""
        )

        prefs?.edit()?.apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_AUTH_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_USER_NAME)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_PHONE)
            apply()
        }
    }

    fun isLoggedIn(): Boolean = _isLoggedInState.value

    fun getCurrentUser(): User = _currentUserState.value
}
