package com.aether.x.core.shell

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Menjalankan perintah shell lewat root (su), didukung oleh libsu — kompatibel
 * dengan Magisk, KernelSU, maupun APatch karena semuanya menyediakan binary
 * `su` standar yang dicari libsu lewat PATH.
 */
class RootShellExecutor : ShellExecutor {

    override val backendName: String = "Root"

    override suspend fun exec(command: String): ShellResult = withContext(Dispatchers.IO) {
        try {
            val result = Shell.cmd(command).exec()
            ShellResult(success = result.isSuccess, output = result.out, error = result.err)
        } catch (t: Throwable) {
            ShellResult.failure(t.message ?: "Gagal menjalankan perintah lewat root")
        }
    }
}
