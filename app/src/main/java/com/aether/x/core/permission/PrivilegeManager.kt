package com.aether.x.core.permission

import android.content.pm.PackageManager
import com.aether.x.core.shell.RootShellExecutor
import com.aether.x.core.shell.ShellExecutor
import com.aether.x.core.shell.ShizukuShellExecutor
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/**
 * Sumber tunggal kebenaran untuk status akses privilese (Shizuku & root).
 * Diinisialisasi sekali dari [com.aether.x.AetherXApp] dan dipakai di seluruh layar.
 */
object PrivilegeManager {

    private const val SHIZUKU_PERMISSION_REQUEST_CODE = 7821

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val _status = MutableStateFlow(PrivilegeStatus())
    val status: StateFlow<PrivilegeStatus> = _status.asStateFlow()

    private var initialized = false

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener { refreshShizuku() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener { refreshShizuku() }
    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, _ ->
            if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) refreshShizuku()
        }

    /** Panggil sekali saat aplikasi dibuat. Aman dipanggil berkali-kali. */
    fun init() {
        if (initialized) return
        initialized = true

        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)

        refreshShizuku()
        checkRootSilently()
    }

    /** Cek ulang status Shizuku (binder hidup + izin) tanpa memunculkan dialog apapun. */
    fun refreshShizuku() {
        val alive = try {
            Shizuku.pingBinder()
        } catch (t: Throwable) {
            false
        }
        val granted = if (alive) {
            try {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } catch (t: Throwable) {
                false
            }
        } else {
            false
        }
        _status.update { it.copy(shizukuAvailable = alive, shizukuGranted = granted) }
    }

    /** Memicu dialog izin Shizuku. Tidak melakukan apa-apa jika server belum hidup. */
    fun requestShizukuPermission() {
        if (!try { Shizuku.pingBinder() } catch (t: Throwable) { false }) return
        if (try { Shizuku.isPreV11() } catch (t: Throwable) { true }) return

        val granted = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (t: Throwable) {
            false
        }
        if (granted) {
            refreshShizuku()
        } else {
            try {
                Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
            } catch (t: Throwable) {
                // Server mati di antara pingBinder() dan requestPermission(); abaikan,
                // status akan diperbarui lewat binderDeadListener.
            }
        }
    }

    /** Cek cepat & non-intrusif: apakah root sudah pernah disetujui sebelumnya. */
    fun checkRootSilently() {
        scope.launch {
            val granted = withContext(Dispatchers.IO) {
                try {
                    Shell.isAppGrantedRoot() == true
                } catch (t: Throwable) {
                    false
                }
            }
            _status.update { it.copy(rootAvailable = granted, rootGranted = granted) }
        }
    }

    /** Memicu prompt superuser (su) dari Magisk/KernelSU/APatch. */
    fun requestRoot() {
        scope.launch {
            _status.update { it.copy(checkingRoot = true) }
            val granted = withContext(Dispatchers.IO) {
                try {
                    Shell.getShell().isRoot
                } catch (t: Throwable) {
                    false
                }
            }
            _status.update { it.copy(rootAvailable = granted, rootGranted = granted, checkingRoot = false) }
        }
    }

    /** Executor aktif sesuai backend yang sedang punya akses, atau null jika belum ada. */
    fun getExecutor(): ShellExecutor? = when (status.value.activeBackend) {
        PrivilegeBackend.SHIZUKU -> ShizukuShellExecutor()
        PrivilegeBackend.ROOT -> RootShellExecutor()
        PrivilegeBackend.NONE -> null
    }
}
