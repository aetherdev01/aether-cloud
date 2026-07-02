package com.aether.x.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.aether.x.ui.components.StatusPill
import com.aether.x.ui.theme.AccentBlue
import com.aether.x.ui.theme.AccentRed
import com.aether.x.ui.theme.StrokeSubtle
import com.aether.x.ui.theme.TextMuted
import com.aether.x.ui.theme.TextPrimary
import com.aether.x.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Status membership yang dipakai murni untuk tampilan di kartu ini —
 * BUKAN gerbang yang mengunci fitur apa pun. Lihat KDoc [MembershipSection]
 * untuk konteks lengkapnya.
 */
enum class MembershipUiStatus { CHECKING, INACTIVE, ACTIVE, EXPIRED }

/**
 * Kartu "Membership" di tab Settings — SATU-SATUNYA tempat lisensi
 * ditampilkan/diaktivasi di aplikasi ini.
 *
 * Sengaja BUKAN gerbang wajib: aplikasi (termasuk semua tweak di tab utama)
 * tetap 100% bisa dipakai tanpa lisensi aktif sama sekali. Kartu ini murni
 * status/badge + form aktivasi opsional, mirip menu "Upgrade ke Premium" di
 * banyak aplikasi umum — bukan blocking screen sebelum masuk aplikasi.
 *
 * Penegakan sesungguhnya (satu kode = satu device, tidak bisa dipakai ulang
 * di device lain) tetap ada di [com.aether.x.data.LicenseRepository] dan
 * firestore.rules; yang berubah di sini hanyalah KAPAN dan DI MANA
 * pengguna berinteraksi dengan lisensinya — bukan mekanisme validasinya.
 */
@Composable
fun MembershipSection(
    status: MembershipUiStatus,
    expiresAtMillis: Long?,
    keyInput: String,
    onKeyInputChange: (String) -> Unit,
    errorMessage: String?,
    isSubmitting: Boolean,
    onActivate: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = statusHeadline(status),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Text(
                    text = statusSubtitle(status, expiresAtMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            StatusBadge(status)
        }

        // Form aktivasi hanya ditampilkan kalau belum aktif — kalau sudah
        // ACTIVE, kartu cukup menunjukkan status + tanggal berakhir, tidak
        // perlu form lagi (mengaktivasi ulang kode yang sama di device yang
        // sama tetap aman dilakukan lewat LicenseRepository kalau nanti mau
        // ditambah tombol "Cek ulang status").
        if (status != MembershipUiStatus.ACTIVE) {
            OutlinedTextField(
                value = keyInput,
                onValueChange = onKeyInputChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("AETX-XXXX-XXXX-XXXX", color = TextMuted) },
                singleLine = true,
                enabled = !isSubmitting,
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
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentRed,
                )
            }

            Button(
                onClick = onActivate,
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Aktivasi Membership", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: MembershipUiStatus) {
    when (status) {
        MembershipUiStatus.CHECKING -> Spacer(modifier = Modifier.height(1.dp))
        MembershipUiStatus.ACTIVE -> StatusPill(
            text = "Aktif",
            containerColor = Color(0xFF1E3A2A),
            contentColor = Color(0xFF6EE7A8),
            dotColor = Color(0xFF6EE7A8),
        )
        MembershipUiStatus.INACTIVE -> StatusPill(
            text = "Belum Aktif",
            dotColor = TextMuted,
        )
        MembershipUiStatus.EXPIRED -> StatusPill(
            text = "Kedaluwarsa",
            containerColor = Color(0xFF3A1E1E),
            contentColor = AccentRed,
            dotColor = AccentRed,
        )
    }
}

private fun statusHeadline(status: MembershipUiStatus): String = when (status) {
    MembershipUiStatus.CHECKING -> "Memeriksa status..."
    MembershipUiStatus.ACTIVE -> "Membership Aktif"
    MembershipUiStatus.INACTIVE -> "Belum Ada Membership"
    MembershipUiStatus.EXPIRED -> "Membership Berakhir"
}

private fun statusSubtitle(status: MembershipUiStatus, expiresAtMillis: Long?): String = when (status) {
    MembershipUiStatus.CHECKING -> "Menyambungkan ke server..."
    MembershipUiStatus.ACTIVE -> expiresAtMillis?.let { "Berlaku sampai ${formatDate(it)}" }
        ?: "Berlaku aktif"
    MembershipUiStatus.INACTIVE -> "Masukkan kode untuk mengaktifkan membership di perangkat ini."
    MembershipUiStatus.EXPIRED -> expiresAtMillis?.let { "Berakhir pada ${formatDate(it)} — masukkan kode baru." }
        ?: "Masukkan kode baru untuk mengaktifkan kembali."
}

private fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID"))
    return formatter.format(Date(millis))
}
