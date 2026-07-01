package com.aether.x.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Sumber ID pengguna GLOBAL yang sebenarnya: setiap install baru mengambil
 * angka urut berikutnya dari satu dokumen counter di Firestore lewat
 * transaksi atomik (`meta/user_counter`, field `count`), sehingga nomornya
 * benar-benar mencerminkan urutan/jumlah total pengguna, bukan angka acak.
 *
 * Sekali berhasil dialokasikan, ID tersebut disimpan permanen secara lokal
 * ([AetherXPreferences.setSyncedUserId]) supaya panggilan berikutnya tidak
 * perlu ke jaringan lagi dan nilainya tidak pernah berubah.
 *
 * Kalau perangkat sedang offline saat pertama kali dibuka, dipakai ID acak
 * lokal sementara ([AetherXPreferences.getOrCreateUserId]) supaya UI tetap
 * punya sesuatu untuk ditampilkan; ID ini akan otomatis disinkronkan ke ID
 * Firestore yang asli begitu koneksi tersedia lagi.
 */
class UserIdRepository(private val preferences: AetherXPreferences) {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val counterRef by lazy { firestore.collection("meta").document("user_counter") }

    private companion object {
        const val TAG = "UserIdRepository"
    }

    suspend fun resolveUserId(): Int {
        preferences.getSyncedUserId()?.let { return it }

        val allocated = runCatching { allocateFromFirestore() }
            .onFailure { e ->
                // Kalau ini muncul di Logcat sebagai PERMISSION_DENIED, artinya
                // Firestore rules belum mengizinkan tulis ke meta/user_counter —
                // cek tab Rules di Firebase Console.
                Log.w(TAG, "Gagal alokasi ID dari Firestore, pakai fallback lokal", e)
            }
            .getOrNull()
        if (allocated != null) {
            preferences.setSyncedUserId(allocated)
            return allocated
        }

        // Offline / gagal menghubungi Firestore: pakai fallback lokal untuk saat ini.
        return preferences.getOrCreateUserId()
    }

    private suspend fun allocateFromFirestore(): Int = suspendCancellableCoroutine { cont ->
        firestore.runTransaction { txn ->
            val snapshot = txn.get(counterRef)
            val current = snapshot.getLong("count") ?: 0L
            val next = current + 1
            txn.set(counterRef, mapOf("count" to next))
            next
        }.addOnSuccessListener { next ->
            if (cont.isActive) cont.resume(next.toInt())
        }.addOnFailureListener { error ->
            if (cont.isActive) cont.resumeWithException(error)
        }
    }
}
