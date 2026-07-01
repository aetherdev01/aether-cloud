package com.aether.x.core.monitor

import android.view.Choreographer
import kotlin.math.roundToInt

/**
 * Menghitung FPS dari kecepatan callback vsync sistem lewat [Choreographer.FrameCallback].
 *
 * PENTING: ini mengukur refresh rate/vsync yang diterima proses AetherX sendiri
 * (sinkron dengan layar), BUKAN frame rate internal render game lain secara langsung —
 * Android tidak mengizinkan aplikasi biasa membaca FPS proses lain tanpa root/instrumentasi
 * khusus. Dalam praktiknya angka ini tetap jadi indikator FPS layar yang cukup akurat
 * (mendekati apa yang dilihat mata), karena vsync callback berjalan mengikuti refresh
 * rate aktual yang dipakai sistem saat itu.
 */
class FpsCounter(private val onFpsUpdated: (Int) -> Unit) {

    private val choreographer = Choreographer.getInstance()
    private var frameCount = 0
    private var windowStartNanos = 0L
    private var running = false

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return

            if (windowStartNanos == 0L) {
                windowStartNanos = frameTimeNanos
            }

            frameCount++
            val elapsedNanos = frameTimeNanos - windowStartNanos

            // Update setiap ~1 detik supaya angka FPS stabil dan mudah dibaca,
            // bukan berkedip-kedip di tiap frame.
            if (elapsedNanos >= 1_000_000_000L) {
                val elapsedSeconds = elapsedNanos / 1_000_000_000f
                val fps = (frameCount / elapsedSeconds).roundToInt()
                onFpsUpdated(fps)
                frameCount = 0
                windowStartNanos = frameTimeNanos
            }

            choreographer.postFrameCallback(this)
        }
    }

    fun start() {
        if (running) return
        running = true
        frameCount = 0
        windowStartNanos = 0L
        choreographer.postFrameCallback(frameCallback)
    }

    fun stop() {
        running = false
        choreographer.removeFrameCallback(frameCallback)
    }
}
