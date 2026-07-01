package com.aether.x.data

import com.aether.x.core.shell.ShellExecutor
import com.aether.x.core.shell.ShellResult

/**
 * Menerjemahkan nilai tweak (DPI, resolusi, pointer speed, refresh rate) menjadi
 * perintah shell Android resmi (`wm`, `settings`). Semua perintah ini sama
 * persis dengan yang dipakai lewat `adb shell`, hanya dijalankan lewat
 * [ShellExecutor] (Shizuku atau root) alih-alih kabel USB.
 */
class TweakRepository {

    suspend fun applyDensity(executor: ShellExecutor, dpi: Int): ShellResult =
        executor.exec("wm density $dpi")

    suspend fun resetDensity(executor: ShellExecutor): ShellResult =
        executor.exec("wm density reset")

    suspend fun applySize(executor: ShellExecutor, widthPx: Int, heightPx: Int): ShellResult =
        executor.exec("wm size ${widthPx}x${heightPx}")

    suspend fun resetSize(executor: ShellExecutor): ShellResult =
        executor.exec("wm size reset")

    suspend fun applyPointerSpeed(executor: ShellExecutor, speed: Int): ShellResult =
        executor.exec("settings put system pointer_speed $speed")

    /**
     * Eksperimental: key ini hanya berdampak nyata di sebagian perangkat
     * (mis. beberapa seri Samsung). Kegagalan di sini tidak dianggap fatal
     * karena memang tidak semua ROM mendukungnya.
     */
    suspend fun applyTouchBoost(executor: ShellExecutor, enabled: Boolean): ShellResult {
        val value = if (enabled) 1 else 0
        return executor.exec("settings put secure touch_sensitivity_enable $value")
    }

    suspend fun applyRefreshRate(executor: ShellExecutor, enabled: Boolean, maxHz: Float): ShellResult {
        return if (enabled) {
            executor.exec("settings put system peak_refresh_rate $maxHz; settings put system min_refresh_rate $maxHz")
        } else {
            executor.exec("settings delete system peak_refresh_rate; settings delete system min_refresh_rate")
        }
    }

    /**
     * Mode Game: mengaktifkan Do Not Disturb (zen mode) sistem lewat `settings put global
     * zen_mode`, supaya notifikasi tidak mengganggu saat bermain. zen_mode 1 = DND penuh
     * (Total Silence-setara di sebagian ROM), 0 = kembali normal. Sama seperti perintah
     * `adb shell settings put global zen_mode 1`, hanya dijalankan lewat Shizuku/root.
     */
    suspend fun applyGameMode(executor: ShellExecutor, enabled: Boolean): ShellResult {
        val value = if (enabled) 1 else 0
        return executor.exec("settings put global zen_mode $value")
    }

    suspend fun resetAll(executor: ShellExecutor): List<ShellResult> = listOf(
        resetDensity(executor),
        resetSize(executor),
        applyPointerSpeed(executor, 0),
        applyTouchBoost(executor, false),
        applyRefreshRate(executor, enabled = false, maxHz = 60f),
        applyGameMode(executor, enabled = false),
    )
}
