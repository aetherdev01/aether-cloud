package com.aether.x.core.permission

enum class PrivilegeBackend { SHIZUKU, ROOT, NONE }

/**
 * Snapshot kondisi akses privilese AetherX saat ini.
 *
 * - [shizukuAvailable] = server Shizuku/Sui sedang berjalan (binder hidup).
 * - [shizukuGranted]   = izin Shizuku untuk AetherX sudah disetujui.
 * - [rootAvailable]    = null berarti belum pernah dicek, true/false setelah dicek.
 * - [rootGranted]      = akses root untuk AetherX sudah disetujui.
 */
data class PrivilegeStatus(
    val shizukuAvailable: Boolean = false,
    val shizukuGranted: Boolean = false,
    val rootAvailable: Boolean? = null,
    val rootGranted: Boolean = false,
    val checkingRoot: Boolean = false,
    val writeSettingsGranted: Boolean = false,
    val overlayGranted: Boolean = false,
    val notificationsGranted: Boolean = false,
) {
    val activeBackend: PrivilegeBackend
        get() = when {
            shizukuAvailable && shizukuGranted -> PrivilegeBackend.SHIZUKU
            rootGranted -> PrivilegeBackend.ROOT
            else -> PrivilegeBackend.NONE
        }

    val hasAccess: Boolean get() = activeBackend != PrivilegeBackend.NONE

    /** Semua izin pendukung (di luar Shizuku/root) sudah aktif. */
    val hasAllSupportingPermissions: Boolean
        get() = writeSettingsGranted && overlayGranted && notificationsGranted
}
