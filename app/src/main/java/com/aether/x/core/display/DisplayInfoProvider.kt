package com.aether.x.core.display

import android.content.Context
import android.os.Build
import android.view.WindowManager

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
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display ?: wm.defaultDisplay
        } else {
            @Suppress("DEPRECATION")
            wm.defaultDisplay
        }

        val metrics = context.resources.displayMetrics
        val widthPx = metrics.widthPixels
        val heightPx = metrics.heightPixels
        val densityDpi = metrics.densityDpi

        val refreshRates = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                display.supportedModes.map { it.refreshRate }.distinct().sorted()
            } else {
                listOf(display.refreshRate)
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
