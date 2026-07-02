package com.aether.x.data

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings

/**
 * Sumber tunggal untuk membaca `ANDROID_ID` perangkat ini. Dipakai oleh
 * [DeviceRegistry] (pendataan device ke Firestore) dan [LicenseRepository]
 * (pengunci lisensi per-device) supaya keduanya selalu memakai nilai yang
 * identik — bukan dua implementasi terpisah yang bisa diam-diam berbeda.
 *
 * Lihat KDoc di [DeviceRegistry] untuk penjelasan kenapa ANDROID_ID (bukan
 * serial number/IMEI) yang dipilih sebagai identifier.
 */
object DeviceId {
    @SuppressLint("HardwareIds")
    fun read(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown"
}
