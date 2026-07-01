package com.aether.x.core.shell

import rikka.shizuku.ShizukuRemoteProcess

/**
 * `Shizuku.newProcess(...)` sudah ditandai deprecated oleh upstream (diarahkan ke
 * UserService), tapi sampai versi shizuku-api 13.1.5 method-nya masih ada secara
 * fisik di binary — hanya disembunyikan dari API publik. Memanggilnya lewat
 * reflection membuat AetherX tetap bisa menjalankan perintah shell sederhana
 * (wm density, settings put, dst.) tanpa perlu membangun AIDL UserService yang
 * jauh lebih kompleks untuk kasus penggunaan yang sebenarnya cukup sederhana ini.
 *
 * Jika suatu saat method ini benar-benar dihapus dari Shizuku, pemanggilan akan
 * gagal dengan NoSuchMethodException yang ditangani dengan baik oleh
 * [ShizukuShellExecutor] (dikembalikan sebagai [ShellResult] gagal, bukan crash).
 */
internal object ShizukuProcessCompat {

    @Throws(Exception::class)
    fun newProcess(cmd: Array<String>, env: Array<String>?, dir: String?): ShizukuRemoteProcess {
        val clazz = Class.forName("rikka.shizuku.Shizuku")
        val method = clazz.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java,
        )
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(null, cmd, env, dir) as ShizukuRemoteProcess
    }
}
