package com.aether.x.ui.components

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * CompositionLocal yang menentukan apakah haptic feedback aktif secara
 * global di aplikasi (mis. dikontrol dari halaman Pengaturan). Default-nya
 * `true` sehingga komponen yang belum di-wrap secara eksplisit tetap
 * mendapat haptic seperti biasa.
 */
val LocalHapticEnabled = compositionLocalOf { true }

/**
 * Memicu [HapticFeedback.performHapticFeedback] hanya jika [enabled] bernilai
 * true. Dipakai oleh TweakSlider/TweakSwitch agar haptic bisa dimatikan lewat
 * [LocalHapticEnabled] tanpa mengubah pemanggilan di tiap komponen.
 */
fun HapticFeedback.performIfEnabled(enabled: Boolean, type: HapticFeedbackType) {
    if (enabled) {
        performHapticFeedback(type)
    }
}
