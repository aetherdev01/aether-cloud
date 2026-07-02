package com.aether.x.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aether.x.data.AetherXPreferences
import com.aether.x.data.LicenseRepository
import com.aether.x.data.LicenseResult
import com.aether.x.ui.theme.AccentBlue
import com.aether.x.ui.theme.AccentRed
import com.aether.x.ui.theme.BgVoid
import com.aether.x.ui.theme.StrokeSubtle
import com.aether.x.ui.theme.SurfaceCard
import com.aether.x.ui.theme.TextMuted
import com.aether.x.ui.theme.TextPrimary
import com.aether.x.ui.theme.TextSecondary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class GateStep { CHECKING, NEEDS_KEY, BLOCKED, SUBMITTING }

/**
 * Gerbang lisensi — ditampilkan SEBELUM [com.aether.x.ui.main.MainScreen]
 * bisa diakses. Tiga kondisi ditangani di sini:
 *
 * 1. Belum pernah aktivasi (tidak ada cache lokal) → tampilkan form input kode.
 * 2. Ada cache lokal, sedang divalidasi ulang ke Firestore (memastikan admin
 *    belum revoke/status belum berubah sejak terakhir dicek).
 * 3. Lisensi terbukti tidak valid (expired/revoked/dipakai device lain) →
 *    layar terkunci total, tidak ada jalan ke MainScreen selain memasukkan
 *    kode yang valid.
 *
 * Pemblokiran di sini murni UX/navigasi di sisi client. Penegakan yang
 * sesungguhnya (mencegah device lain memakai kode yang sama) ada di
 * firestore.rules + transaksi atomik di [LicenseRepository.activate].
 */
@Composable
fun LicenseGateScreen(
    onLicenseValid: () -> Unit,
) {
    val context = LocalContext.current
    val preferences = remember { AetherXPreferences(context) }
    val licenseRepository = remember { LicenseRepository(context) }
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(GateStep.CHECKING) }
    var keyInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var blockedMessage by remember { mutableStateOf<String?>(null) }

    // Saat layar ini pertama kali muncul: kalau ada cache key lokal, validasi
    // ulang ke server dulu (bisa saja sudah di-revoke/expired sejak terakhir
    // dicek). Kalau tidak ada cache sama sekali, langsung minta input kode.
    LaunchedEffect(Unit) {
        val cachedKey = preferences.preferences.first().licenseKey

        if (cachedKey.isNullOrBlank()) {
            step = GateStep.NEEDS_KEY
            return@LaunchedEffect
        }

        when (val result = licenseRepository.revalidate(cachedKey)) {
            is LicenseResult.Valid -> {
                preferences.setLicenseCache(cachedKey, result.expiresAtMillis)
                onLicenseValid()
            }
            is LicenseResult.Expired -> {
                preferences.clearLicenseCache()
                blockedMessage = "Lisensi sudah kedaluwarsa pada ${formatDate(result.expiredAtMillis)}. Masukkan kode baru untuk melanjutkan."
                step = GateStep.BLOCKED
            }
            LicenseResult.Revoked -> {
                preferences.clearLicenseCache()
                blockedMessage = "Lisensi ini sudah dicabut. Hubungi admin kalau menurutmu ini keliru."
                step = GateStep.BLOCKED
            }
            LicenseResult.BoundToOtherDevice -> {
                preferences.clearLicenseCache()
                blockedMessage = "Kode ini sudah terpakai di perangkat lain. Satu kode hanya berlaku untuk satu perangkat."
                step = GateStep.BLOCKED
            }
            LicenseResult.NotFound -> {
                preferences.clearLicenseCache()
                step = GateStep.NEEDS_KEY
            }
            LicenseResult.NetworkError -> {
                // Offline/gagal jaringan: JANGAN kunci pengguna keluar dari app
                // hanya karena tidak ada koneksi. Cache lokal masih dipercaya
                // sampai validasi berikutnya berhasil.
                onLicenseValid()
            }
        }
    }

    fun submitKey() {
        val trimmed = keyInput.trim()
        if (trimmed.isEmpty()) {
            errorMessage = "Masukkan kode lisensi terlebih dulu."
            return
        }
        errorMessage = null
        step = GateStep.SUBMITTING
        scope.launch {
            when (val result = licenseRepository.activate(trimmed)) {
                is LicenseResult.Valid -> {
                    preferences.setLicenseCache(trimmed, result.expiresAtMillis)
                    onLicenseValid()
                }
                is LicenseResult.Expired -> {
                    errorMessage = "Kode ini sudah kedaluwarsa sejak ${formatDate(result.expiredAtMillis)}."
                    step = GateStep.NEEDS_KEY
                }
                LicenseResult.Revoked -> {
                    errorMessage = "Kode ini sudah dicabut oleh admin."
                    step = GateStep.NEEDS_KEY
                }
                LicenseResult.BoundToOtherDevice -> {
                    errorMessage = "Kode ini sudah dipakai di perangkat lain."
                    step = GateStep.NEEDS_KEY
                }
                LicenseResult.NotFound -> {
                    errorMessage = "Kode tidak ditemukan. Periksa lagi penulisannya."
                    step = GateStep.NEEDS_KEY
                }
                LicenseResult.NetworkError -> {
                    errorMessage = "Gagal terhubung ke server. Periksa koneksi internet lalu coba lagi."
                    step = GateStep.NEEDS_KEY
                }
            }
        }
    }

    Scaffold(containerColor = BgVoid) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            when (step) {
                GateStep.CHECKING -> {
                    CircularProgressIndicator(color = AccentBlue)
                }

                GateStep.NEEDS_KEY, GateStep.SUBMITTING, GateStep.BLOCKED -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val isBlocked = step == GateStep.BLOCKED
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(if (isBlocked) AccentRed.copy(alpha = 0.15f) else SurfaceCard),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = if (isBlocked) AccentRed else AccentBlue,
                                modifier = Modifier.size(28.dp),
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = if (isBlocked) "Akses Terkunci" else "Masukkan Kode Lisensi",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = blockedMessage.takeIf { isBlocked }
                                ?: "AetherX perlu kode lisensi aktif untuk membuka tab Tweak. Kode ini hanya bisa dipakai di satu perangkat.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            value = keyInput,
                            onValueChange = {
                                keyInput = it
                                errorMessage = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("AETX-XXXX-XXXX-XXXX", color = TextMuted) },
                            singleLine = true,
                            enabled = step != GateStep.SUBMITTING,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            isError = errorMessage != null,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = StrokeSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                errorBorderColor = AccentRed,
                            ),
                        )

                        errorMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentRed,
                                textAlign = TextAlign.Center,
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { submitKey() },
                            enabled = step != GateStep.SUBMITTING,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        ) {
                            if (step == GateStep.SUBMITTING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text("Aktivasi", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID"))
    return formatter.format(Date(millis))
}
