package com.aether.x.core.shell

/**
 * Hasil eksekusi satu perintah shell, dari backend manapun (Shizuku atau root).
 */
data class ShellResult(
    val success: Boolean,
    val output: List<String> = emptyList(),
    val error: List<String> = emptyList(),
) {
    val outputText: String get() = output.joinToString("\n")
    val errorText: String get() = error.joinToString("\n")

    companion object {
        fun failure(message: String) = ShellResult(success = false, error = listOf(message))
    }
}

/**
 * Abstraksi sumber privilese untuk menjalankan perintah shell (wm, settings, dll).
 * Implementasinya bisa lewat Shizuku (ADB) atau root (su).
 */
interface ShellExecutor {
    val backendName: String

    suspend fun exec(command: String): ShellResult
}
