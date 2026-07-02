package com.aether.x.core.monitor

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.roundToInt

/**
 * Membaca statistik CPU load, suhu, dan GPU load dari /proc, /sys, dan API resmi.
 * FPS dibaca terpisah oleh [com.aether.x.core.monitor.GfxInfoFpsReader] lewat
 * `dumpsys gfxinfo`, bukan di sini.
 */
class SystemStatsProvider {

    private var lastCpuTotal: Long = -1
    private var lastCpuIdle: Long = -1

    /** Load CPU rata-rata seluruh core dalam persen (0-100), atau null kalau gagal baca. */
    fun readCpuLoadPercent(): Int? = runCatching {
        RandomAccessFile("/proc/stat", "r").use { reader ->
            val line = reader.readLine() ?: return null
            // Format: "cpu  user nice system idle iowait irq softirq steal ..."
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.isEmpty() || parts[0] != "cpu") return null
            val values = parts.drop(1).mapNotNull { it.toLongOrNull() }
            if (values.size < 4) return null

            val idle = values[3] + (values.getOrNull(4) ?: 0L) // idle + iowait
            val total = values.sum()

            if (lastCpuTotal < 0) {
                lastCpuTotal = total
                lastCpuIdle = idle
                return null // butuh 2 sampel untuk hitung delta
            }

            val totalDelta = total - lastCpuTotal
            val idleDelta = idle - lastCpuIdle
            lastCpuTotal = total
            lastCpuIdle = idle

            if (totalDelta <= 0) return null
            val busyPercent = ((totalDelta - idleDelta).toFloat() / totalDelta.toFloat()) * 100f
            busyPercent.roundToInt().coerceIn(0, 100)
        }
    }.getOrNull()

    /**
     * Suhu baterai/perangkat dalam Celsius, dibaca lewat sticky intent
     * ACTION_BATTERY_CHANGED (API resmi Android, EXTRA_TEMPERATURE dalam
     * persepuluh derajat Celsius). Fallback ke thermal zone kalau gagal.
     */
    fun readTemperatureCelsius(context: Context): Float? {
        val fromBattery = runCatching {
            val intent = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            )
            val tenthsOfCelsius = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
            if (tenthsOfCelsius > 0) tenthsOfCelsius / 10f else null
        }.getOrNull()

        return fromBattery ?: readThermalZoneCelsius()
    }

    private fun readThermalZoneCelsius(): Float? = runCatching {
        val thermalDir = File("/sys/class/thermal")
        val zones = thermalDir.listFiles { f -> f.name.startsWith("thermal_zone") } ?: return null

        // Cari zona dengan nilai yang masuk akal untuk suhu perangkat (10-100°C),
        // karena beberapa zona dipakai untuk sensor lain (baterai, kamera, dll)
        // dengan skala berbeda-beda antar vendor.
        for (zone in zones.sortedBy { it.name }) {
            val tempFile = File(zone, "temp")
            if (!tempFile.canRead()) continue
            val raw = tempFile.readText().trim().toLongOrNull() ?: continue
            // Kebanyakan vendor melaporkan dalam milli-Celsius (mis. 35000 = 35.0°C),
            // sebagian kecil langsung dalam Celsius (mis. 35).
            val celsius = if (raw > 1000) raw / 1000f else raw.toFloat()
            if (celsius in 10f..100f) return celsius
        }
        null
    }.getOrNull()

    /**
     * Load GPU dalam persen, hanya tersedia di sebagian chipset (terutama Qualcomm
     * Adreno lewat kgsl). Kembalikan null kalau node tidak ada — UI akan
     * menampilkan "-" alih-alih angka palsu.
     */
    fun readGpuLoadPercent(): Int? = runCatching {
        val candidatePaths = listOf(
            "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage", // Adreno
            "/sys/kernel/gpu/gpu_busy",
            "/sys/class/devfreq/gpufreq/gpu_busy",
        )
        for (path in candidatePaths) {
            val file = File(path)
            if (!file.canRead()) continue
            val text = file.readText().trim()
            // gpu_busy_percentage biasanya berformat "12 %" atau "12%"
            val numeric = Regex("\\d+").find(text)?.value?.toIntOrNull() ?: continue
            return numeric.coerceIn(0, 100)
        }
        null
    }.getOrNull()
}
