package com.aether.x.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "aetherx_prefs")

enum class DarkModePref { SYSTEM, LIGHT, DARK }

data class AppPreferences(
    val onboardingCompleted: Boolean = false,
    val dynamicColorEnabled: Boolean = true,
    val darkModePref: DarkModePref = DarkModePref.SYSTEM,
    val dpiValue: Int = -1,
    val widthValue: Int = -1,
    val pointerSpeed: Int = 0,
    val touchBoostEnabled: Boolean = false,
    val forceMaxRefreshRate: Boolean = false,
)

/**
 * Sumber kebenaran untuk preferensi pengguna: status onboarding, preferensi
 * tampilan (tema), dan nilai tweak terakhir yang diterapkan/disimpan.
 */
class AetherXPreferences(private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color_enabled")
        val DARK_MODE = stringPreferencesKey("dark_mode_pref")
        val DPI_VALUE = intPreferencesKey("dpi_value")
        val WIDTH_VALUE = intPreferencesKey("width_value")
        val POINTER_SPEED = intPreferencesKey("pointer_speed")
        val TOUCH_BOOST = booleanPreferencesKey("touch_boost_enabled")
        val FORCE_REFRESH = booleanPreferencesKey("force_max_refresh_rate")
        // Disimpan agar bisa dipulihkan walau aplikasi sempat ditutup,
        // meski nilainya berupa Float (refresh rate target dalam Hz).
        val REFRESH_TARGET = floatPreferencesKey("refresh_target_hz")
    }

    val preferences: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        AppPreferences(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            dynamicColorEnabled = prefs[Keys.DYNAMIC_COLOR] ?: true,
            darkModePref = prefs[Keys.DARK_MODE]?.let { runCatching { DarkModePref.valueOf(it) }.getOrNull() }
                ?: DarkModePref.SYSTEM,
            dpiValue = prefs[Keys.DPI_VALUE] ?: -1,
            widthValue = prefs[Keys.WIDTH_VALUE] ?: -1,
            pointerSpeed = prefs[Keys.POINTER_SPEED] ?: 0,
            touchBoostEnabled = prefs[Keys.TOUCH_BOOST] ?: false,
            forceMaxRefreshRate = prefs[Keys.FORCE_REFRESH] ?: false,
        )
    }

    suspend fun setOnboardingCompleted(value: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = value }
    }

    suspend fun setDynamicColorEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = value }
    }

    suspend fun setDarkModePref(value: DarkModePref) {
        context.dataStore.edit { it[Keys.DARK_MODE] = value.name }
    }

    suspend fun saveTweakState(
        dpiValue: Int,
        pointerSpeed: Int,
        touchBoostEnabled: Boolean,
        forceMaxRefreshRate: Boolean,
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DPI_VALUE] = dpiValue
            prefs[Keys.POINTER_SPEED] = pointerSpeed
            prefs[Keys.TOUCH_BOOST] = touchBoostEnabled
            prefs[Keys.FORCE_REFRESH] = forceMaxRefreshRate
        }
    }

    suspend fun clearTweakState() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.DPI_VALUE)
            prefs.remove(Keys.WIDTH_VALUE)
            prefs[Keys.POINTER_SPEED] = 0
            prefs[Keys.TOUCH_BOOST] = false
            prefs[Keys.FORCE_REFRESH] = false
        }
    }
}
