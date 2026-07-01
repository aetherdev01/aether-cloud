package com.aether.x.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.random.Random

private val Context.dataStore by preferencesDataStore(name = "aetherx_prefs")

enum class DarkModePref { SYSTEM, LIGHT, DARK }

enum class CrosshairStyle { CROSS, DOT, CIRCLE, CIRCLE_DOT, PLUS_GAP, X_SHAPE }

enum class FpsMonitorStyle { ROG, CLASSIC }

enum class TemperatureUnit { CELSIUS, FAHRENHEIT }

data class AppPreferences(
    val onboardingCompleted: Boolean = false,
    val dynamicColorEnabled: Boolean = false,
    val darkModePref: DarkModePref = DarkModePref.SYSTEM,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val dpiValue: Int = -1,
    val widthValue: Int = -1,
    val pointerSpeed: Int = 0,
    val touchBoostEnabled: Boolean = false,
    val forceMaxRefreshRate: Boolean = false,
    val gameModeEnabled: Boolean = false,
    val crosshairEnabled: Boolean = false,
    val crosshairStyle: CrosshairStyle = CrosshairStyle.CROSS,
    val crosshairColor: Long = 0xFF00FF66,
    val crosshairSize: Int = 32,
    val crosshairThickness: Int = 3,
    val crosshairOpacity: Int = 100,
    val crosshairOffsetX: Int = 0,
    val crosshairOffsetY: Int = 0,
    val fpsMonitorEnabled: Boolean = false,
    val fpsMonitorStyle: FpsMonitorStyle = FpsMonitorStyle.CLASSIC,
    // Offset hanya dipakai oleh gaya ROG (bisa digeser). Gaya Classic selalu
    // terkunci di pojok kiri bawah layar, tidak memakai offset ini.
    val fpsMonitorOffsetX: Int = 0,
    val fpsMonitorOffsetY: Int = 0,
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
        val TEMPERATURE_UNIT = stringPreferencesKey("temperature_unit")
        val DPI_VALUE = intPreferencesKey("dpi_value")
        val WIDTH_VALUE = intPreferencesKey("width_value")
        val POINTER_SPEED = intPreferencesKey("pointer_speed")
        val TOUCH_BOOST = booleanPreferencesKey("touch_boost_enabled")
        val FORCE_REFRESH = booleanPreferencesKey("force_max_refresh_rate")
        val GAME_MODE = booleanPreferencesKey("game_mode_enabled")
        // Disimpan agar bisa dipulihkan walau aplikasi sempat ditutup,
        // meski nilainya berupa Float (refresh rate target dalam Hz).
        val REFRESH_TARGET = floatPreferencesKey("refresh_target_hz")

        val CROSSHAIR_ENABLED = booleanPreferencesKey("crosshair_enabled")
        val CROSSHAIR_STYLE = stringPreferencesKey("crosshair_style")
        val CROSSHAIR_COLOR = longPreferencesKey("crosshair_color")
        val CROSSHAIR_SIZE = intPreferencesKey("crosshair_size")
        val CROSSHAIR_THICKNESS = intPreferencesKey("crosshair_thickness")
        val CROSSHAIR_OPACITY = intPreferencesKey("crosshair_opacity")
        val CROSSHAIR_OFFSET_X = intPreferencesKey("crosshair_offset_x")
        val CROSSHAIR_OFFSET_Y = intPreferencesKey("crosshair_offset_y")

        val FPS_MONITOR_ENABLED = booleanPreferencesKey("fps_monitor_enabled")
        val FPS_MONITOR_STYLE = stringPreferencesKey("fps_monitor_style")
        val FPS_MONITOR_OFFSET_X = intPreferencesKey("fps_monitor_offset_x")
        val FPS_MONITOR_OFFSET_Y = intPreferencesKey("fps_monitor_offset_y")

        // ID pengguna lokal (mis. "ID-67128") yang ditampilkan sebagai pengganti
        // status Shizuku/Root di tab Tweak. Dibuat sekali secara acak lalu
        // disimpan permanen di perangkat supaya nilainya konsisten setiap dibuka.
        val USER_ID = intPreferencesKey("user_id")

        // true kalau USER_ID di atas adalah nomor urut ASLI hasil alokasi dari
        // counter Firestore (lihat UserIdRepository) — bukan sekadar angka acak
        // fallback lokal yang dibuat saat offline.
        val USER_ID_SYNCED = booleanPreferencesKey("user_id_synced")
    }

    val preferences: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        AppPreferences(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            dynamicColorEnabled = prefs[Keys.DYNAMIC_COLOR] ?: false,
            darkModePref = prefs[Keys.DARK_MODE]?.let { runCatching { DarkModePref.valueOf(it) }.getOrNull() }
                ?: DarkModePref.SYSTEM,
            temperatureUnit = prefs[Keys.TEMPERATURE_UNIT]
                ?.let { runCatching { TemperatureUnit.valueOf(it) }.getOrNull() }
                ?: TemperatureUnit.CELSIUS,
            dpiValue = prefs[Keys.DPI_VALUE] ?: -1,
            widthValue = prefs[Keys.WIDTH_VALUE] ?: -1,
            pointerSpeed = prefs[Keys.POINTER_SPEED] ?: 0,
            touchBoostEnabled = prefs[Keys.TOUCH_BOOST] ?: false,
            forceMaxRefreshRate = prefs[Keys.FORCE_REFRESH] ?: false,
            gameModeEnabled = prefs[Keys.GAME_MODE] ?: false,
            crosshairEnabled = prefs[Keys.CROSSHAIR_ENABLED] ?: false,
            crosshairStyle = prefs[Keys.CROSSHAIR_STYLE]
                ?.let { runCatching { CrosshairStyle.valueOf(it) }.getOrNull() }
                ?: CrosshairStyle.CROSS,
            crosshairColor = prefs[Keys.CROSSHAIR_COLOR] ?: 0xFF00FF66,
            crosshairSize = prefs[Keys.CROSSHAIR_SIZE] ?: 32,
            crosshairThickness = prefs[Keys.CROSSHAIR_THICKNESS] ?: 3,
            crosshairOpacity = prefs[Keys.CROSSHAIR_OPACITY] ?: 100,
            crosshairOffsetX = prefs[Keys.CROSSHAIR_OFFSET_X] ?: 0,
            crosshairOffsetY = prefs[Keys.CROSSHAIR_OFFSET_Y] ?: 0,
            fpsMonitorEnabled = prefs[Keys.FPS_MONITOR_ENABLED] ?: false,
            fpsMonitorStyle = prefs[Keys.FPS_MONITOR_STYLE]
                ?.let { runCatching { FpsMonitorStyle.valueOf(it) }.getOrNull() }
                ?: FpsMonitorStyle.CLASSIC,
            fpsMonitorOffsetX = prefs[Keys.FPS_MONITOR_OFFSET_X] ?: 0,
            fpsMonitorOffsetY = prefs[Keys.FPS_MONITOR_OFFSET_Y] ?: 0,
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

    suspend fun setTemperatureUnit(value: TemperatureUnit) {
        context.dataStore.edit { it[Keys.TEMPERATURE_UNIT] = value.name }
    }

    suspend fun saveTweakState(
        pointerSpeed: Int,
        touchBoostEnabled: Boolean,
        forceMaxRefreshRate: Boolean,
        gameModeEnabled: Boolean,
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.POINTER_SPEED] = pointerSpeed
            prefs[Keys.TOUCH_BOOST] = touchBoostEnabled
            prefs[Keys.FORCE_REFRESH] = forceMaxRefreshRate
            prefs[Keys.GAME_MODE] = gameModeEnabled
        }
    }

    suspend fun clearTweakState() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.DPI_VALUE)
            prefs.remove(Keys.WIDTH_VALUE)
            prefs[Keys.POINTER_SPEED] = 0
            prefs[Keys.TOUCH_BOOST] = false
            prefs[Keys.FORCE_REFRESH] = false
            prefs[Keys.GAME_MODE] = false
        }
    }

    suspend fun setCrosshairEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.CROSSHAIR_ENABLED] = value }
    }

    suspend fun saveCrosshairConfig(
        style: CrosshairStyle,
        color: Long,
        size: Int,
        thickness: Int,
        opacity: Int,
        offsetX: Int,
        offsetY: Int,
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CROSSHAIR_STYLE] = style.name
            prefs[Keys.CROSSHAIR_COLOR] = color
            prefs[Keys.CROSSHAIR_SIZE] = size
            prefs[Keys.CROSSHAIR_THICKNESS] = thickness
            prefs[Keys.CROSSHAIR_OPACITY] = opacity
            prefs[Keys.CROSSHAIR_OFFSET_X] = offsetX
            prefs[Keys.CROSSHAIR_OFFSET_Y] = offsetY
        }
    }

    suspend fun setCrosshairOffset(offsetX: Int, offsetY: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CROSSHAIR_OFFSET_X] = offsetX
            prefs[Keys.CROSSHAIR_OFFSET_Y] = offsetY
        }
    }

    suspend fun setFpsMonitorEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.FPS_MONITOR_ENABLED] = value }
    }

    suspend fun setFpsMonitorStyle(style: FpsMonitorStyle) {
        context.dataStore.edit { it[Keys.FPS_MONITOR_STYLE] = style.name }
    }

    suspend fun setFpsMonitorOffset(offsetX: Int, offsetY: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FPS_MONITOR_OFFSET_X] = offsetX
            prefs[Keys.FPS_MONITOR_OFFSET_Y] = offsetY
        }
    }

    /**
     * Mengambil ID pengguna lokal yang tersimpan, atau membuatnya sekali kalau
     * belum ada (angka acak 1..99999, ditampilkan sebagai "ID-<angka>"). Ini
     * dipakai sebagai fallback sementara saat perangkat offline — begitu ada
     * koneksi, [UserIdRepository] akan menggantinya dengan ID urut asli dari
     * Firestore lewat [setSyncedUserId].
     */
    suspend fun getOrCreateUserId(): Int {
        val existing = context.dataStore.data.first()[Keys.USER_ID]
        if (existing != null) return existing

        val generated = Random.nextInt(1, 100_000)
        context.dataStore.edit { prefs -> prefs[Keys.USER_ID] = generated }
        return generated
    }

    /** ID urut asli hasil alokasi Firestore, kalau sudah pernah berhasil disinkronkan. */
    suspend fun getSyncedUserId(): Int? {
        val prefs = context.dataStore.data.first()
        return if (prefs[Keys.USER_ID_SYNCED] == true) prefs[Keys.USER_ID] else null
    }

    /** Menyimpan ID urut asli dari Firestore sebagai nilai permanen ID pengguna. */
    suspend fun setSyncedUserId(id: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_ID] = id
            prefs[Keys.USER_ID_SYNCED] = true
        }
    }
}
