package com.aether.x.core.display

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display

data class DisplayInfo(
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int,
    val supportedRefreshRates: List<Float>,
) {
    val maxRefreshRate: Float get() = supportedRefreshRates.maxOrNull() ?: 60f
    val aspectRatio: Float get() = heightPx.toFloat() / widthPx.toFloat()
}

/**
 * Membaca resolusi, densitas, dan refresh rate yang didukung layar fisik
 * memakai API Android biasa (tidak butuh Shizuku/root). Ini dipakai sebagai
 * acuan "default pabrik" untuk slider Tweak dan untuk tombol Reset.
 */
object DisplayInfoProvider {

    fun read(context: Context): DisplayInfo {
        // PENTING: jangan pakai context.display / WindowManager.defaultDisplay di sini.
        // TweakViewModel memanggil fungsi ini dengan Application context (getApplication()),
        // dan mulai Android R (API 30), context.display akan langsung melempar
        // UnsupportedOperationException kalau context-nya bukan context "visual"
        // (Activity / WindowContext). Application context BUKAN context visual,
        // jadi ini menyebabkan force close instan saat home screen dibuka.
        // DisplayManager aman dipakai dari context jenis apa pun.
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        val display: Display? = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)

        val metrics = context.resources.displayMetrics
        val widthPx = metrics.widthPixels
        val heightPx = metrics.heightPixels
        val densityDpi = metrics.densityDpi

        val refreshRates = try {
            when {
                display == null -> listOf(60f)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                    display.supportedModes.map { it.refreshRate }.distinct().sorted()
                else -> listOf(display.refreshRate)
            }
        } catch (t: Throwable) {
            listOf(60f)
        }.ifEmpty { listOf(60f) }

        return DisplayInfo(
            widthPx = widthPx,
            heightPx = heightPx,
            densityDpi = densityDpi,
            supportedRefreshRates = refreshRates,
        )
    }
}
