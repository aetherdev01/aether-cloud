package com.aether.x.core.monitor

import com.aether.x.core.shell.ShellExecutor
import kotlin.math.roundToInt

/**
 * Membaca FPS RENDER SUNGGUHAN dari aplikasi game yang sedang berjalan di
 * foreground (mis. Free Fire), lewat `dumpsys gfxinfo <package> framestats` —
 * perintah resmi Android yang sama dipakai tools profiling seperti Android
 * Studio Profiler / GPU Overdraw.
 *
 * INI BERBEDA dari pendekatan lama yang memakai Choreographer.FrameCallback:
 * Choreographer hanya mengukur vsync callback milik proses AetherX SENDIRI,
 * yang pada dasarnya sama dengan refresh rate layar (Hz) — bukan FPS render
 * game lain. Karena itu angkanya selalu terlihat "rapi" (60, 90, 120) dan
 * tidak pernah turun walau game sedang berat, alias tidak akurat.
 *
 * `dumpsys gfxinfo` membaca timestamp presentasi frame ASLI dari SurfaceFlinger
 * untuk proses target, sehingga bisa menampilkan drop FPS yang sebenarnya saat
 * game lag/stutter. Perintah ini butuh akses shell (Shizuku atau root), yang
 * memang sudah menjadi prasyarat fitur-fitur lain di AetherX.
 */
class GfxInfoFpsReader(private val packageName: String) {

    companion object {
        // Kolom ke-14 (index 13) dari setiap baris frame di section "---PROFILEDATA---"
        // adalah FRAME_COMPLETED (nanodetik) sejak boot — dokumentasi resmi format
        // framestats Android (frameworks/base FrameInfo.java).
        private const val FRAME_COMPLETED_COLUMN_INDEX = 13
        private const val EXPECTED_MIN_COLUMNS = 14
        private const val NANOS_PER_SECOND = 1_000_000_000.0
        private const val FRAME_WINDOW = 30
    }

    /**
     * Mengambil satu snapshot FPS rata-rata dari beberapa frame terakhir yang
     * dirender oleh [packageName]. Mengembalikan null kalau game tidak sedang
     * berjalan, tidak ada data frame terbaru, atau perintah gagal dieksekusi.
     */
    suspend fun readFps(executor: ShellExecutor): Int? {
        val result = executor.exec("dumpsys gfxinfo $packageName framestats")
        if (!result.success) return null

        val timestampsNanos = parseFrameCompletedTimestamps(result.output)
        if (timestampsNanos.size < 2) return null

        // Hanya pakai jendela beberapa frame terakhir supaya angka mencerminkan
        // kondisi "sekarang", bukan rata-rata sepanjang sesi yang sudah lama berjalan.
        val recent = timestampsNanos.takeLast(FRAME_WINDOW)
        if (recent.size < 2) return null

        val elapsedNanos = recent.last() - recent.first()
        if (elapsedNanos <= 0) return null

        val frameIntervals = recent.size - 1
        val fps = (frameIntervals * NANOS_PER_SECOND) / elapsedNanos
        if (fps.isNaN() || fps.isInfinite()) return null

        // FPS render nyata dibatasi ke rentang wajar; di luar itu kemungkinan
        // besar data framestats basi/rusak (mis. game baru saja dibuka).
        return fps.roundToInt().coerceIn(0, 240)
    }

    private fun parseFrameCompletedTimestamps(lines: List<String>): List<Long> {
        val timestamps = mutableListOf<Long>()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || !trimmed[0].isDigit()) continue

            val columns = trimmed.split(',')
            if (columns.size <= FRAME_COMPLETED_COLUMN_INDEX) continue
            if (columns.size < EXPECTED_MIN_COLUMNS) continue

            val completedNanos = columns[FRAME_COMPLETED_COLUMN_INDEX].trim().toLongOrNull() ?: continue
            if (completedNanos <= 0) continue
            timestamps.add(completedNanos)
        }
        return timestamps.sorted()
    }
}
