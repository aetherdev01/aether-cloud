package com.aether.x.core.monitor

import com.aether.x.core.shell.ShellExecutor

/**
 * Mendeteksi package name aplikasi yang SEDANG di foreground (topmost/focused),
 * lewat `dumpsys activity activities` (fallback ke `dumpsys window`).
 *
 * Ini memperbaiki bug Monitor FPS yang selalu menunjukkan 0: sebelumnya AetherX
 * hanya mengecek APK mana yang TERPASANG (lewat PackageManager) satu kali saat
 * overlay pertama dinyalakan, lalu memakai package itu terus-menerus untuk
 * `dumpsys gfxinfo <package> framestats`. Kalau game belum dibuka sama sekali,
 * baru dibuka setelah overlay aktif, atau pengguna pindah ke aplikasi lain,
 * `dumpsys gfxinfo` tidak punya data frame terbaru untuk package yang salah
 * sasaran itu → `GfxInfoFpsReader` selalu mengembalikan null/0.
 *
 * Dengan membaca app yang benar-benar di foreground setiap siklus refresh,
 * FPS yang ditampilkan selalu mengikuti aplikasi yang sedang benar-benar
 * dipakai/dilihat pengguna saat itu.
 */
class ForegroundAppReader {

    /**
     * Mengembalikan package name aplikasi foreground saat ini, atau null kalau
     * tidak bisa dideteksi (mis. perintah shell gagal atau formatnya berubah
     * di ROM tertentu).
     */
    suspend fun readForegroundPackage(executor: ShellExecutor): String? {
        readFromActivities(executor)?.let { return it }
        readFromWindow(executor)?.let { return it }
        return null
    }

    private suspend fun readFromActivities(executor: ShellExecutor): String? {
        val result = executor.exec("dumpsys activity activities")
        if (!result.success) return null
        return parseResumedPackage(result.output)
    }

    private suspend fun readFromWindow(executor: ShellExecutor): String? {
        val result = executor.exec("dumpsys window")
        if (!result.success) return null
        return parseFocusedWindowPackage(result.output)
    }

    companion object {
        // Contoh baris nyata (format bisa sedikit beda antar versi Android):
        //   "  ResumedActivity: ActivityRecord{... com.dts.freefireth/... t123}"
        //   "  mResumedActivity: ActivityRecord{... com.dts.freefireth/.MainActivity t123}"
        private val RESUMED_ACTIVITY_REGEX =
            Regex("""[Rr]esumedActivity:\s*ActivityRecord\{[^ ]+\s+[^ ]+\s+([a-zA-Z0-9_.]+)/""")

        // Contoh baris nyata:
        //   "  mCurrentFocus=Window{... com.dts.freefireth/com.dts.freefireth.MainActivity}"
        //   "  mFocusedApp=ActivityRecord{... com.dts.freefireth/... t123}"
        private val FOCUSED_WINDOW_REGEX =
            Regex("""m(CurrentFocus|FocusedApp)=\w*\{[^ ]+\s+([a-zA-Z0-9_.]+)/""")

        internal fun parseResumedPackage(lines: List<String>): String? {
            for (line in lines) {
                RESUMED_ACTIVITY_REGEX.find(line)?.let { match ->
                    return match.groupValues[1].takeIf { it.isNotBlank() }
                }
            }
            return null
        }

        internal fun parseFocusedWindowPackage(lines: List<String>): String? {
            for (line in lines) {
                FOCUSED_WINDOW_REGEX.find(line)?.let { match ->
                    return match.groupValues[2].takeIf { it.isNotBlank() }
                }
            }
            return null
        }
    }
}
