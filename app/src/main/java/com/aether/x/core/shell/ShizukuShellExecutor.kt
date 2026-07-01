package com.aether.x.core.shell

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Menjalankan perintah shell lewat Shizuku (privilese ADB/shell, atau root
 * jika Shizuku sendiri dijalankan sebagai root / lewat Sui).
 */
class ShizukuShellExecutor : ShellExecutor {

    override val backendName: String = "Shizuku"

    override suspend fun exec(command: String): ShellResult = withContext(Dispatchers.IO) {
        try {
            val process = ShizukuProcessCompat.newProcess(arrayOf("sh", "-c", command), null, null)
            val out = process.inputStream.bufferedReader().readLines()
            val err = process.errorStream.bufferedReader().readLines()
            val exitCode = process.waitFor()
            process.destroy()
            ShellResult(success = exitCode == 0, output = out, error = err)
        } catch (t: Throwable) {
            ShellResult.failure(t.message ?: "Gagal menjalankan perintah lewat Shizuku")
        }
    }
}
