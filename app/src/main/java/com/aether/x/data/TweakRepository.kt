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

    /**
     * Khusus root: kunci semua core CPU ke governor "performance" (clock
     * selalu maksimum) selama bermain, mengurangi micro-stutter akibat
     * frekuensi naik-turun. Dikembalikan ke "schedutil" (default umum kernel
     * modern) saat dimatikan. Butuh akses tulis ke /sys/devices/system/cpu,
     * yang biasanya hanya bisa lewat root (bukan Shizuku/adb shell biasa).
     */
    suspend fun applyCpuPerformanceMode(executor: ShellExecutor, enabled: Boolean): ShellResult {
        val governor = if (enabled) "performance" else "schedutil"
        return executor.exec(
            "for g in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo $governor > \$g; done",
        )
    }

    /**
     * Khusus root: turunkan swappiness kernel supaya sistem lebih jarang
     * menukar (swap) data game ke zram/disk, menjaga proses game tetap di
     * RAM. Dikembalikan ke nilai default (60) saat dimatikan. Butuh akses
     * tulis ke /proc/sys/vm, yang biasanya hanya bisa lewat root.
     */
    suspend fun applyRamPriority(executor: ShellExecutor, enabled: Boolean): ShellResult {
        val value = if (enabled) 10 else 60
        return executor.exec("echo $value > /proc/sys/vm/swappiness")
    }

    /**
     * Khusus root: menaikkan batas suhu shutdown/throttle di zona termal
     * kernel supaya sistem tidak buru-buru menurunkan clock CPU/GPU saat
     * perangkat mulai panas bermain lama. Nilai ditulis dalam milidegree
     * Celsius (mis. 90000 = 90°C) ke setiap `trip_point_0_temp` yang ada.
     * Tidak ada nilai "default pabrik" yang seragam antar chipset, jadi saat
     * dimatikan kita tidak menulis ulang apa pun — cukup andalkan reboot
     * atau reset manual perangkat untuk kembali ke batas asli vendor.
     * Butuh akses tulis ke /sys/class/thermal, hanya bisa lewat root.
     */
    suspend fun applyThermalThrottleOverride(executor: ShellExecutor, enabled: Boolean): ShellResult {
        return if (enabled) {
            executor.exec(
                "for z in /sys/class/thermal/thermal_zone*/trip_point_0_temp; do " +
                    "echo 90000 > \$z; done",
            )
        } else {
            executor.exec("echo 'thermal override dimatikan, restart perangkat untuk memulihkan batas asli vendor'")
        }
    }

    /**
     * Khusus root: kunci frekuensi GPU ke nilai maksimum yang didukung
     * (governor "performance"), mirip cara kerja Mode Performa CPU. Path
     * governor GPU tidak seragam antar chipset (Adreno/Mali/dsb), jadi
     * perintah ini mencoba beberapa lokasi umum sekaligus — yang tidak ada
     * di perangkat akan otomatis diabaikan shell tanpa membuat proses gagal.
     * Dikembalikan ke "simple_ondemand"/"default" saat dimatikan.
     * Butuh akses tulis ke /sys/class/kgsl atau /sys/devices/[chip]/kgsl-3d0,
     * yang biasanya hanya bisa lewat root.
     */
    suspend fun applyGpuPerformanceMode(executor: ShellExecutor, enabled: Boolean): ShellResult {
        val governor = if (enabled) "performance" else "simple_ondemand"
        return executor.exec(
            "for g in /sys/class/kgsl/kgsl-3d0/devfreq/governor " +
                "/sys/devices/platform/*/kgsl-3d0/devfreq/governor; do " +
                "[ -f \$g ] && echo $governor > \$g; done",
        )
    }

    suspend fun resetAll(executor: ShellExecutor): List<ShellResult> = listOf(
        resetDensity(executor),
        resetSize(executor),
        applyPointerSpeed(executor, 0),
        applyTouchBoost(executor, false),
        applyRefreshRate(executor, enabled = false, maxHz = 60f),
        applyGameMode(executor, enabled = false),
        applyCpuPerformanceMode(executor, enabled = false),
        applyRamPriority(executor, enabled = false),
        applyThermalThrottleOverride(executor, enabled = false),
        applyGpuPerformanceMode(executor, enabled = false),
    )
}
