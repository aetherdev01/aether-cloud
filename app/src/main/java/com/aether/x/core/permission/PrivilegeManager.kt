package com.aether.x.core.permission

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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

    /**
     * Otomatis meminta akses (dialog Shizuku dan/atau prompt su root) tanpa perlu
     * layar "Siapkan Akses" terpisah. Dipanggil dari splash screen saat startup.
     * Root dicoba lebih dulu secara silent (banyak perangkat sudah pernah approve),
     * lalu Shizuku diminta kalau server-nya sudah hidup dan izinnya belum diberikan.
     * Aman dipanggil berkali-kali; tidak melakukan apa-apa kalau salah satu backend
     * sudah aktif.
     */
    suspend fun autoRequestAccess() {
        // Root: cek dulu secara silent, baru minta prompt su kalau memang belum ada.
        if (!_status.value.rootGranted) {
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

        // Shizuku: hanya minta dialog izin kalau server-nya memang sudah berjalan
        // (mis. lewat Wireless Debugging / Sui) dan izin belum disetujui.
        refreshShizuku()
        if (!_status.value.shizukuGranted && _status.value.shizukuAvailable) {
            requestShizukuPermission()
        }
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

    /**
     * Cek cepat & non-intrusif: apakah root sudah pernah disetujui sebelumnya.
     *
     * PENTING: sengaja memakai `Shell.getShell().isRoot` (bukan
     * `Shell.isAppGrantedRoot()`) karena yang terakhir hanya membaca status
     * shell yang SUDAH ada di proses ini — begitu proses app baru dimulai
     * (mis. app dibuka ulang / di-swipe lalu dibuka lagi), belum ada shell
     * sama sekali sehingga selalu balik `false`/`null` walau root sebenarnya
     * masih diizinkan. `Shell.getShell()` benar-benar mencoba membangun shell
     * root; kalau sudah pernah disetujui sebelumnya, Magisk/KernelSU/APatch
     * akan meloloskannya tanpa dialog apapun — jadi tetap "silent" di mata
     * pengguna, tapi hasilnya akurat.
     */
    fun checkRootSilently() {
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

    /**
     * Cek ulang SEMUA backend privilese (Shizuku + root) sekaligus, tanpa
     * memunculkan dialog apapun. Dipanggil tiap kali app kembali ke
     * foreground (lihat [com.aether.x.MainActivity]) supaya status akses
     * tidak pernah "basi" — mis. kalau root/Shizuku sempat dicabut atau
     * server Shizuku sempat mati lalu hidup lagi saat app di-background.
     */
    fun refreshAll() {
        refreshShizuku()
        checkRootSilently()
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

    // ---------------------------------------------------------------------
    // Izin pendukung: "Ubah Pengaturan Sistem" (WRITE_SETTINGS), overlay,
    // dan notifikasi. Bukan pengganti Shizuku/root, tapi membantu beberapa
    // tweak (mis. baca/tulis Settings.System langsung dari proses AetherX,
    // overlay crosshair/FPS, dan notifikasi foreground service) berjalan
    // lebih stabil di berbagai ROM. Semua dicek ulang tiap kali splash
    // screen tampil dan tiap kali app kembali ke foreground.
    // ---------------------------------------------------------------------

    /** Cek ulang ketiga izin pendukung sekaligus tanpa memunculkan dialog apapun. */
    fun refreshSupportingPermissions(context: Context) {
        val writeSettings = Settings.System.canWrite(context)
        val overlay = Settings.canDrawOverlays(context)
        val notifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        _status.update {
            it.copy(
                writeSettingsGranted = writeSettings,
                overlayGranted = overlay,
                notificationsGranted = notifications,
            )
        }
    }

    /** Buka halaman sistem untuk memberi izin "Ubah Pengaturan Sistem" (WRITE_SETTINGS). */
    fun requestWriteSettings(context: Context) {
        if (Settings.System.canWrite(context)) return
        val intent = Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /** Buka halaman sistem untuk memberi izin "Tampil di atas aplikasi lain" (overlay). */
    fun requestOverlayPermission(context: Context) {
        if (Settings.canDrawOverlays(context)) return
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
