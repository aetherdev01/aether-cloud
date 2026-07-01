package com.aether.x.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Menyimpan preferensi getaran umpan balik pengguna (dari Settings > Interaksi)
 * supaya komponen interaktif (switch, tombol) di seluruh app bisa langsung tahu
 * apakah harus bergetar tanpa perlu menerima parameter tambahan satu-satu.
 *
 * Default true (menyala) supaya perilaku tetap konsisten kalau provider belum
 * dipasang di suatu bagian layar (misal preview/test).
 */
val LocalHapticEnabled = staticCompositionLocalOf { true }

/**
 * Memicu getaran umpan balik standar (klik/toggle) HANYA jika preferensi haptic
 * pengguna aktif. Dipakai sebagai pengganti pemanggilan langsung
 * `HapticFeedback.performHapticFeedback` di seluruh komponen interaktif.
 */
fun HapticFeedback.performIfEnabled(enabled: Boolean, type: HapticFeedbackType = HapticFeedbackType.LongPress) {
    if (enabled) performHapticFeedback(type)
}
