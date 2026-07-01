package com.aether.x.data

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Mendata setiap perangkat yang membuka aplikasi ke Firestore, dikunci per
 * `ANDROID_ID` (Settings.Secure.ANDROID_ID) sebagai document ID di koleksi
 * `devices`. Perlu dicatat sifat identifier ini:
 *
 * - ANDROID_ID unik per kombinasi app-signing key + akun pengguna + perangkat,
 *   BUKAN serial number/IMEI (yang aksesnya sudah dibatasi ketat sejak
 *   Android 10 dan dilarang dipakai untuk analytics/tracking oleh kebijakan
 *   Google Play).
 * - Nilainya di-reset kalau aplikasi di-uninstall lalu dipasang ulang (sejak
 *   Android 8), jadi ini BUKAN identifier permanen yang tidak bisa direset
 *   pengguna.
 * - Tidak butuh permission khusus untuk dibaca.
 *
 * Field `firstLoginAt` sengaja ditulis dengan `FieldValue.serverTimestamp()`
 * dan hanya dipasang lewat `SetOptions.merge()` — kalau field itu sudah ada
 * di dokumen, Firestore tidak menimpanya lagi kecuali kita override manual.
 * Untuk memastikan hanya tertulis SEKALI (login pertama yang sesungguhnya,
 * bukan setiap kali app dibuka), kita cek dulu keberadaan dokumennya sebelum
 * memutuskan apakah field ini perlu disertakan.
 */
class DeviceRegistry(private val context: Context) {

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private companion object {
        const val TAG = "DeviceRegistry"
        const val COLLECTION = "devices"
    }

    @SuppressLint("HardwareIds")
    val deviceId: String
        get() = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown"

    /**
     * Menulis/memperbarui dokumen perangkat ini di Firestore. Dipanggil sekali
     * per sesi aplikasi (mis. dari [TweakViewModel] saat pertama kali dibuka).
     * Gagal secara diam-diam (dicatat ke Logcat) supaya tidak pernah memblokir
     * alur utama aplikasi — pendataan ini bersifat best-effort.
     *
     * @param userId ID pengguna numerik lokal/tersinkron (lihat [UserIdRepository]).
     */
    suspend fun recordDeviceLogin(userId: Int?) {
        runCatching {
            val docRef = firestore.collection(COLLECTION).document(deviceId)
            val isFirstLogin = !documentExists(docRef)

            val data = buildMap<String, Any> {
                put("deviceId", deviceId)
                put("lastLoginAt", FieldValue.serverTimestamp())
                if (userId != null) put("userId", userId)
                if (isFirstLogin) put("firstLoginAt", FieldValue.serverTimestamp())
            }

            setDocument(docRef, data)
        }.onFailure { e ->
            Log.w(TAG, "Gagal mendata perangkat ke Firestore", e)
        }
    }

    private suspend fun documentExists(
        docRef: com.google.firebase.firestore.DocumentReference,
    ): Boolean = suspendCancellableCoroutine { cont ->
        docRef.get()
            .addOnSuccessListener { snapshot -> if (cont.isActive) cont.resume(snapshot.exists()) }
            .addOnFailureListener { if (cont.isActive) cont.resume(false) }
    }

    private suspend fun setDocument(
        docRef: com.google.firebase.firestore.DocumentReference,
        data: Map<String, Any>,
    ): Unit = suspendCancellableCoroutine { cont ->
        docRef.set(data, SetOptions.merge())
            .addOnSuccessListener { if (cont.isActive) cont.resume(Unit) }
            .addOnFailureListener { if (cont.isActive) cont.resume(Unit) }
    }
}
