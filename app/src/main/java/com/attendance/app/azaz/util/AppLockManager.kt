package com.attendance.app.azaz.util

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
        const val DEFAULT_PIN = "0000"
        val APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val IS_LOCK_ENABLED = booleanPreferencesKey("is_lock_enabled")
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
        val sp = context.getSharedPreferences("haazri_prefs", Context.MODE_PRIVATE)
        sp.edit()
            .putString("app_lock_pin", pin)
            .putBoolean("is_lock_enabled", true)
            .apply()
        context.dataStore.edit { prefs ->
            prefs[APP_LOCK_PIN] = pin
            prefs[IS_LOCK_ENABLED] = true
        }
    }

    suspend fun setLockEnabled(enabled: Boolean) {
        val sp = context.getSharedPreferences("haazri_prefs", Context.MODE_PRIVATE)
        val editor = sp.edit().putBoolean("is_lock_enabled", enabled)
        if (enabled && sp.getString("app_lock_pin", null).isNullOrBlank()) {
            editor.putString("app_lock_pin", DEFAULT_PIN)
        }
        editor.apply()

        context.dataStore.edit { prefs ->
            prefs[IS_LOCK_ENABLED] = enabled
            if (enabled && prefs[APP_LOCK_PIN].isNullOrBlank()) {
                prefs[APP_LOCK_PIN] = DEFAULT_PIN
            }
        }
    }

    suspend fun resetPinToDefault() {
        setPin(DEFAULT_PIN)
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        val sp = context.getSharedPreferences("haazri_prefs", Context.MODE_PRIVATE)
        sp.edit().putBoolean("app_lock_biometric", enabled).apply()
        context.dataStore.edit { prefs ->
            prefs[BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun clearLock() {
        val sp = context.getSharedPreferences("haazri_prefs", Context.MODE_PRIVATE)
        sp.edit()
            .remove("app_lock_pin")
            .remove("app_lock_biometric")
            .putBoolean("is_lock_enabled", false)
            .apply()
        context.dataStore.edit { prefs ->
            prefs.remove(APP_LOCK_PIN)
            prefs.remove(BIOMETRIC_ENABLED)
            prefs[IS_LOCK_ENABLED] = false
        }
    }

    fun getSavedPinSync(): String {
        val prefs = context.getSharedPreferences("haazri_prefs", Context.MODE_PRIVATE)
        return prefs.getString("app_lock_pin", DEFAULT_PIN) ?: DEFAULT_PIN
    }

    fun isBiometricEnabledSync(): Boolean {
        val prefs = context.getSharedPreferences("haazri_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("app_lock_biometric", false)
    }

    fun isLockEnabledSync(): Boolean {
        val prefs = context.getSharedPreferences("haazri_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("is_lock_enabled", prefs.contains("app_lock_pin"))
    }

    fun validatePin(input: String): Boolean {
        return input == getSavedPinSync()
    }

    val isLockEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_LOCK_ENABLED] ?: isLockEnabledSync()
    }

    val pinFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[APP_LOCK_PIN] ?: getSavedPinSync()
    }

    val biometricFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[BIOMETRIC_ENABLED] ?: isBiometricEnabledSync()
    }

    fun canAuthenticateWithBiometrics(): Boolean {
        return try {
            val biometricManager = androidx.biometric.BiometricManager.from(context)
            biometricManager.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
        } catch (_: Exception) {
            false
        }
    }

    fun hasPromptedAppLock(): Boolean {
        val prefs = context.getSharedPreferences("haazri_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("has_prompted_app_lock", false)
    }

    fun setPromptedAppLock(prompted: Boolean) {
        val prefs = context.getSharedPreferences("haazri_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("has_prompted_app_lock", prompted).apply()
    }

    suspend fun enableDefaultLock(pin: String = DEFAULT_PIN, biometric: Boolean = false) {
        setPin(pin)
        setBiometricEnabled(biometric)
        setPromptedAppLock(true)
        unlockApp()
    }
}
