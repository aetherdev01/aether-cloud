package com.aether.x.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Sumber ID pengguna GLOBAL yang sebenarnya: setiap install baru mengambil
 * angka urut berikutnya dari satu dokumen counter di Firestore lewat
 * transaksi atomik (`meta/user_counter`, field `count`), sehingga nomornya
 * benar-benar mencerminkan urutan/jumlah total pengguna.
 *
 * SENGAJA TIDAK ADA fallback angka acak. ID pengguna dipakai sebagai
 * identitas yang ditampilkan ke pengguna dan (lewat [DeviceRegistry]) ikut
 * tersimpan permanen di Firestore — angka acak yang "keliru dikira asli"
 * lebih berbahaya daripada sekadar tidak menampilkan apa pun sementara.
 * Kalau alokasi gagal (offline, dsb), [resolveUserId] mencoba lagi beberapa
 * kali dengan jeda yang membesar (exponential backoff), dan kalau tetap
 * gagal, mengembalikan `null` — UI ([TweakScreen]) sudah menangani `userId
 * == null` dengan cara paling aman: pill ID pengguna cuma disembunyikan,
 * bukan menampilkan nilai yang salah.
 *
 * Sekali berhasil dialokasikan, ID tersebut disimpan permanen secara lokal
 * ([AetherXPreferences.setSyncedUserId]) supaya panggilan berikutnya tidak
 * perlu ke jaringan lagi dan nilainya tidak pernah berubah.
 */
class UserIdRepository(private val preferences: AetherXPreferences) {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val counterRef by lazy { firestore.collection("meta").document("user_counter") }

    private companion object {
        const val TAG = "UserIdRepository"
        const val MAX_ATTEMPTS = 4
        const val INITIAL_BACKOFF_MILLIS = 1_000L
    }

    /**
     * Mengembalikan ID pengguna asli, atau `null` kalau setelah [MAX_ATTEMPTS]
     * percobaan (dengan backoff) tetap gagal menghubungi Firestore — TIDAK
     * PERNAH mengembalikan angka acak. Pemanggil (mis. [TweakViewModel])
     * bebas memanggil ulang fungsi ini lagi nanti (mis. saat koneksi pulih)
     * untuk mencoba lagi; setiap panggilan yang gagal tidak meninggalkan efek
     * samping yang perlu dibersihkan.
     */
    suspend fun resolveUserId(): Int? {
        preferences.getSyncedUserId()?.let { return it }

        var backoff = INITIAL_BACKOFF_MILLIS
        repeat(MAX_ATTEMPTS) { attempt ->
            val allocated = runCatching { allocateFromFirestore() }
                .onFailure { e ->
                    // Kalau ini muncul di Logcat sebagai PERMISSION_DENIED, artinya
                    // Firestore rules belum mengizinkan tulis ke meta/user_counter —
                    // cek tab Rules di Firebase Console.
                    Log.w(TAG, "Percobaan ${attempt + 1}/$MAX_ATTEMPTS alokasi ID gagal", e)
                }
                .getOrNull()

            if (allocated != null) {
                preferences.setSyncedUserId(allocated)
                return allocated
            }

            if (attempt < MAX_ATTEMPTS - 1) {
                delay(backoff)
                backoff *= 2
            }
        }

        Log.w(TAG, "Gagal alokasi ID pengguna setelah $MAX_ATTEMPTS percobaan — tidak memakai fallback acak.")
        return null
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
