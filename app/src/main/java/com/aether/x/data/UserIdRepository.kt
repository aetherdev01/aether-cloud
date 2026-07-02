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
 *
 * PEMULIHAN SETELAH UNINSTALL/INSTALL ULANG: preferensi lokal (tempat
 * `userId` disimpan) ikut terhapus saat app di-uninstall, padahal
 * `ANDROID_ID` device pada umumnya tetap sama (lihat catatan di [DeviceId]
 * dan [DeviceRegistry]). Sebelum mengalokasikan nomor BARU dari counter,
 * [resolveUserId] sekarang cek dulu apakah device ini SUDAH punya dokumen di
 * koleksi `devices` (dari sesi sebelum uninstall) — kalau ada dan field
 * `userId`-nya masih tersimpan di sana, angka itu yang dipakai lagi (badge
 * "ID-…" balik ke nomor yang sama, bukan nomor baru atau kosong). Baru kalau
 * device ini benar-benar belum pernah tercatat, counter global dinaikkan.
 */
class UserIdRepository(private val preferences: AetherXPreferences, private val deviceId: String) {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val counterRef by lazy { firestore.collection("meta").document("user_counter") }
    private val deviceRef by lazy { firestore.collection("devices").document(deviceId) }

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
            val resolved = runCatching { resolveExistingOrAllocate() }
                .onFailure { e ->
                    // Kalau ini muncul di Logcat sebagai PERMISSION_DENIED, artinya
                    // Firestore rules belum mengizinkan baca/tulis ke devices/{id}
                    // atau meta/user_counter — cek tab Rules di Firebase Console.
                    Log.w(TAG, "Percobaan ${attempt + 1}/$MAX_ATTEMPTS resolusi ID gagal", e)
                }
                .getOrNull()

            if (resolved != null) {
                preferences.setSyncedUserId(resolved)
                return resolved
            }

            if (attempt < MAX_ATTEMPTS - 1) {
                delay(backoff)
                backoff *= 2
            }
        }

        Log.w(TAG, "Gagal resolusi ID pengguna setelah $MAX_ATTEMPTS percobaan — tidak memakai fallback acak.")
        return null
    }

    /**
     * Cek dulu dokumen `devices/{deviceId}` yang sudah ada (mis. dari sebelum
     * app di-uninstall) — kalau device ini sudah pernah dialokasikan `userId`,
     * pakai lagi nomor itu supaya konsisten setelah install ulang. Hanya kalau
     * device ini benar-benar baru (dokumen belum ada / belum punya `userId`)
     * baru minta nomor baru dari counter global.
     */
    private suspend fun resolveExistingOrAllocate(): Int {
        val existing = fetchExistingUserId()
        if (existing != null) return existing
        return allocateFromFirestore()
    }

    private suspend fun fetchExistingUserId(): Int? = suspendCancellableCoroutine { cont ->
        deviceRef.get()
            .addOnSuccessListener { snapshot ->
                val existing = snapshot.getLong("userId")?.toInt()
                if (cont.isActive) cont.resume(existing)
            }
            .addOnFailureListener { error ->
                // Gagal baca (mis. offline) bukan berarti device belum pernah
                // terdaftar — jangan diam-diam alokasikan nomor baru di sini,
                // biarkan resolveUserId() yang retry lewat backoff di atas.
                if (cont.isActive) cont.resumeWithException(error)
            }
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
