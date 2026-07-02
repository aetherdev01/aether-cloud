package com.aether.x.ui.membership

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aether.x.R
import com.aether.x.ui.components.SectionCard
import com.aether.x.ui.components.StatusPill
import com.aether.x.ui.theme.AccentAmber
import com.aether.x.ui.theme.AccentAmberContainer
import com.aether.x.ui.theme.AccentBlue
import com.aether.x.ui.theme.AccentGreen
import com.aether.x.ui.theme.AccentGreenContainer
import com.aether.x.ui.theme.AccentRed
import com.aether.x.ui.theme.StrokeSubtle
import com.aether.x.ui.theme.SurfaceCardAlt
import com.aether.x.ui.theme.TextMuted
import com.aether.x.ui.theme.TextPrimary
import com.aether.x.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tab Membership tersendiri di bottom navigation — sebelumnya berupa satu
 * kartu di dalam tab Pengaturan (lihat riwayat [MembershipViewModel]),
 * sekarang jadi layar penuh dengan hero status card + form aktivasi + daftar
 * keuntungan, supaya lebih jelas terpisah dari pengaturan umum aplikasi dan
 * lebih rapi secara visual (badge & warna teks konsisten dengan palet resmi
 * di ui/theme/Color.kt, bukan lagi warna mentah yang ditulis inline).
 */
@Composable
fun MembershipScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: MembershipViewModel = viewModel(),
) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    val expiresAtMillis by viewModel.expiresAtMillis.collectAsStateWithLifecycle()
    val keyInput by viewModel.keyInput.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text(
                text = stringResource(R.string.membership_headline),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.membership_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        MembershipHeroCard(status = status, expiresAtMillis = expiresAtMillis)

        if (status != MembershipUiStatus.ACTIVE) {
            SectionCard(title = stringResource(R.string.membership_key_label)) {
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = viewModel::setKeyInput,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.membership_key_placeholder), color = TextMuted) },
                    singleLine = true,
                    enabled = !isSubmitting,
                    // Format lisensi sekarang bebas: huruf besar/kecil dan angka apa
                    // pun (tidak dipaksa satu pola AETX-XXXX-XXXX-XXXX saja seperti
                    // sebelumnya). Kapitalisasi keyboard "Sentences" dipakai murni
                    // supaya keyboard tidak otomatis mengubah huruf jadi UPPERCASE —
                    // apa pun yang diketik pengguna disimpan apa adanya.
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
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
                    onClick = viewModel::activate,
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
                        Text(
                            text = stringResource(R.string.membership_activate_button),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        SectionCard(title = stringResource(R.string.membership_benefits_title)) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                BenefitRow(text = stringResource(R.string.membership_benefit_1))
                BenefitRow(text = stringResource(R.string.membership_benefit_2))
                BenefitRow(text = stringResource(R.string.membership_benefit_3))
            }
        }
    }
}

/**
 * Kartu hero besar di puncak tab Membership: ikon mahkota, badge status warna
 * konsisten (hijau = aktif, kuning = kedaluwarsa, netral = belum aktif), dan
 * subjudul tanggal berlaku/berakhir.
 */
@Composable
private fun MembershipHeroCard(status: MembershipUiStatus, expiresAtMillis: Long?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceCardAlt)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            MembershipIcon(status)
            StatusBadge(status)
        }

        Column {
            Text(
                text = statusHeadline(status),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            Text(
                text = statusSubtitle(status, expiresAtMillis),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun MembershipIcon(status: MembershipUiStatus) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(iconContainerColor(status)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.WorkspacePremium,
            contentDescription = null,
            tint = iconTintColor(status),
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun BenefitRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = AccentGreen,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
    }
}

@Composable
private fun StatusBadge(status: MembershipUiStatus) {
    when (status) {
        MembershipUiStatus.CHECKING -> Spacer(modifier = Modifier.height(1.dp))
        MembershipUiStatus.ACTIVE -> StatusPill(
            text = stringResource(R.string.membership_badge_active),
            containerColor = AccentGreenContainer,
            contentColor = AccentGreen,
            dotColor = AccentGreen,
        )
        MembershipUiStatus.INACTIVE -> StatusPill(
            text = stringResource(R.string.membership_badge_inactive),
            dotColor = TextMuted,
        )
        MembershipUiStatus.EXPIRED -> StatusPill(
            text = stringResource(R.string.membership_badge_expired),
            containerColor = AccentAmberContainer,
            contentColor = AccentAmber,
            dotColor = AccentAmber,
        )
    }
}

@Composable
private fun iconContainerColor(status: MembershipUiStatus) = when (status) {
    MembershipUiStatus.ACTIVE -> AccentGreenContainer
    MembershipUiStatus.EXPIRED -> AccentAmberContainer
    else -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun iconTintColor(status: MembershipUiStatus) = when (status) {
    MembershipUiStatus.ACTIVE -> AccentGreen
    MembershipUiStatus.EXPIRED -> AccentAmber
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun statusHeadline(status: MembershipUiStatus): String = when (status) {
    MembershipUiStatus.CHECKING -> stringResource(R.string.membership_status_checking)
    MembershipUiStatus.ACTIVE -> stringResource(R.string.membership_status_active)
    MembershipUiStatus.INACTIVE -> stringResource(R.string.membership_status_inactive)
    MembershipUiStatus.EXPIRED -> stringResource(R.string.membership_status_expired)
}

@Composable
private fun statusSubtitle(status: MembershipUiStatus, expiresAtMillis: Long?): String = when (status) {
    MembershipUiStatus.CHECKING -> stringResource(R.string.membership_status_checking_desc)
    MembershipUiStatus.ACTIVE -> expiresAtMillis?.let {
        stringResource(R.string.membership_status_active_desc, formatDate(it))
    } ?: stringResource(R.string.membership_status_active_desc_no_date)
    MembershipUiStatus.INACTIVE -> stringResource(R.string.membership_status_inactive_desc)
    MembershipUiStatus.EXPIRED -> expiresAtMillis?.let {
        stringResource(R.string.membership_status_expired_desc, formatDate(it))
    } ?: stringResource(R.string.membership_status_expired_desc_no_date)
}

private fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID"))
    return formatter.format(Date(millis))
}
