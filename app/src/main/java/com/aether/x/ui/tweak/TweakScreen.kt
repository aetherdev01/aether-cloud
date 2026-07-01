package com.aether.x.ui.tweak

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aether.x.R
import com.aether.x.core.permission.PrivilegeBackend
import com.aether.x.core.permission.PrivilegeManager
import com.aether.x.ui.components.SectionCard
import com.aether.x.ui.components.StatusPill
import com.aether.x.ui.components.TweakSlider
import com.aether.x.ui.components.TweakSwitch

@Composable
fun TweakScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: TweakViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val privilegeStatus by PrivilegeManager.status.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Deteksi ulang game terpasang setiap kali layar Tweak kembali aktif
    // (mis. setelah pengguna baru saja memasang Free Fire dari luar app).
    // Tombol buka game sendiri sekarang tampil sebagai FAB di MainScreen.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshDetectedGames()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(contentPadding)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TweakHeader(
                activeBackend = privilegeStatus.activeBackend,
                hasAccess = privilegeStatus.hasAccess,
            )

            SectionCard(title = stringResource(R.string.tweak_section_touch)) {
                // Nilai diterapkan langsung ke sistem saat slider dilepas (tidak perlu
                // tombol "Terapkan" terpisah lagi).
                TweakSlider(
                    label = stringResource(R.string.tweak_pointer_speed),
                    description = stringResource(R.string.tweak_pointer_speed_desc),
                    valueText = state.pointerSpeed.toString(),
                    value = state.pointerSpeed.toFloat(),
                    range = -7f..7f,
                    steps = 13,
                    onValueChange = viewModel::onPointerSpeedChange,
                    onValueChangeFinished = viewModel::onPointerSpeedChangeFinished,
                )
                TweakSwitch(
                    label = stringResource(R.string.tweak_touch_boost),
                    description = stringResource(R.string.tweak_touch_boost_desc),
                    checked = state.touchBoost,
                    onCheckedChange = viewModel::onTouchBoostChange,
                )
            }

            SectionCard(title = stringResource(R.string.tweak_section_refresh)) {
                TweakSwitch(
                    label = stringResource(R.string.tweak_force_refresh),
                    description = stringResource(
                        R.string.tweak_force_refresh_desc,
                    ) + " (${state.displayInfo.maxRefreshRate.toInt()}Hz)",
                    checked = state.forceMaxRefreshRate,
                    onCheckedChange = viewModel::onForceRefreshChange,
                )
            }

            SectionCard(title = stringResource(R.string.tweak_section_game_mode)) {
                TweakSwitch(
                    label = stringResource(R.string.tweak_game_mode),
                    description = stringResource(R.string.tweak_game_mode_desc),
                    checked = state.gameModeEnabled,
                    onCheckedChange = viewModel::onGameModeChange,
                )
            }

            OutlinedButton(
                onClick = viewModel::resetTweaks,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.tweak_reset))
            }
        }
        SnackbarHost(hostState = snackbarHostState)
    }
}

/**
 * Header "hero" di puncak halaman Tweak: judul, subjudul singkat, dan status
 * akses privilese aktif (Shizuku/Root) sebagai pill kecil yang jelas terlihat.
 */
@Composable
private fun TweakHeader(
    activeBackend: PrivilegeBackend,
    hasAccess: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.nav_tweak),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = stringResource(R.string.tweak_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            StatusPill(
                text = when (activeBackend) {
                    PrivilegeBackend.SHIZUKU -> stringResource(R.string.tweak_status_active_shizuku)
                    PrivilegeBackend.ROOT -> stringResource(R.string.tweak_status_active_root)
                    PrivilegeBackend.NONE -> stringResource(R.string.tweak_status_inactive)
                },
                containerColor = if (hasAccess) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface
                },
                contentColor = if (hasAccess) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
