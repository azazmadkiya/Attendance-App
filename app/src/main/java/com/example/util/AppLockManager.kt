package com.example.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "app_settings")

class AppLockManager(private val context: Context) {
    companion object {
        val APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    }

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked = _isUnlocked.asStateFlow()

    fun lockApp() {
        _isUnlocked.value = false
    }

    fun unlockApp() {
        _isUnlocked.value = true
    }

    suspend fun setPin(pin: String) {
        context.dataStore.edit { prefs ->
            prefs[APP_LOCK_PIN] = pin
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun clearLock() {
        context.dataStore.edit { prefs ->
            prefs.remove(APP_LOCK_PIN)
            prefs.remove(BIOMETRIC_ENABLED)
        }
    }

    fun getPin(): Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[APP_LOCK_PIN]
    }

    fun isBiometricEnabled(): Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[BIOMETRIC_ENABLED] ?: false
    }
}
