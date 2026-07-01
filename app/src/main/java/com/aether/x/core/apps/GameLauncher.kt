package com.aether.x.core.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Info satu game yang bisa dideteksi & dibuka langsung dari AetherX
 * (mis. Free Fire, Free Fire Max).
 */
data class DetectedGame(
    val packageName: String,
    val displayName: String,
)

/**
 * Mendeteksi aplikasi game yang relevan (saat ini: Free Fire & Free Fire Max)
 * yang terpasang di perangkat, dan menyediakan cara membukanya langsung.
 */
object GameLauncher {

    const val PACKAGE_FREE_FIRE = "com.dts.freefireth"
    const val PACKAGE_FREE_FIRE_MAX = "com.dts.freefire.max"

    private val knownGames = listOf(
        PACKAGE_FREE_FIRE to "Free Fire",
        PACKAGE_FREE_FIRE_MAX to "Free Fire MAX",
    )

    /** Mengembalikan daftar game yang terpasang di antara [knownGames], bisa kosong. */
    fun detectInstalled(context: Context): List<DetectedGame> {
        val pm = context.packageManager
        return knownGames.mapNotNull { (pkg, name) ->
            if (isInstalled(pm, pkg)) DetectedGame(pkg, name) else null
        }
    }

    private fun isInstalled(pm: PackageManager, packageName: String): Boolean = try {
        pm.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    /** Membuka game lewat launch intent resminya. Mengembalikan false kalau gagal. */
    fun launch(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (t: Throwable) {
            false
        }
    }
}
