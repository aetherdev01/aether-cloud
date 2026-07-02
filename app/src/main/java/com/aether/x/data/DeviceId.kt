package com.aether.x.data

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings

/**
 * Sumber tunggal untuk membaca `ANDROID_ID` perangkat ini. Dipakai oleh
 * [UserIdRepository] (pendataan device + alokasi userId ke Firestore) dan
 * [LicenseRepository] (pengunci lisensi per-device) supaya keduanya selalu
 * memakai nilai yang identik — bukan dua implementasi terpisah yang bisa
 * diam-diam berbeda.
 *
 * CATATAN: ANDROID_ID (Settings.Secure.ANDROID_ID) dipilih sebagai
 * identifier, BUKAN serial number/IMEI (yang aksesnya sudah dibatasi ketat
 * sejak Android 10 dan dilarang dipakai untuk analytics/tracking oleh
 * kebijakan Google Play). Nilainya unik per kombinasi app-signing key + akun
 * pengguna + perangkat, dan di-reset kalau aplikasi di-uninstall lalu
 * dipasang ulang (sejak Android 8) — jadi ini BUKAN identifier permanen yang
 * tidak bisa direset pengguna. Tidak butuh permission khusus untuk dibaca.
 */
object DeviceId {
    @SuppressLint("HardwareIds")
    fun read(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown"
}
