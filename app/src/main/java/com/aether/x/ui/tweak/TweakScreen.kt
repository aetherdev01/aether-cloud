package com.aether.x.ui.tweak

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
    var dpiInput by remember { mutableStateOf(state.dpi.toString()) }

    LaunchedEffect(state.dpi) {
        if (state.dpi.toString() != dpiInput) {
            dpiInput = state.dpi.toString()
        }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(R.string.nav_tweak), style = MaterialTheme.typography.headlineSmall)
                StatusPill(
                    text = when (privilegeStatus.activeBackend) {
                        PrivilegeBackend.SHIZUKU -> stringResource(R.string.tweak_status_active_shizuku)
                        PrivilegeBackend.ROOT -> stringResource(R.string.tweak_status_active_root)
                        PrivilegeBackend.NONE -> stringResource(R.string.tweak_status_inactive)
                    },
                    containerColor = if (privilegeStatus.hasAccess) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = if (privilegeStatus.hasAccess) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            SectionCard(title = stringResource(R.string.tweak_section_touch)) {
                TweakSlider(
                    label = stringResource(R.string.tweak_pointer_speed),
                    description = stringResource(R.string.tweak_pointer_speed_desc),
                    valueText = state.pointerSpeed.toString(),
                    value = state.pointerSpeed.toFloat(),
                    range = -7f..7f,
                    steps = 13,
                    onValueChange = viewModel::onPointerSpeedChange,
                )
                TweakSwitch(
                    label = stringResource(R.string.tweak_touch_boost),
                    description = stringResource(R.string.tweak_touch_boost_desc),
                    checked = state.touchBoost,
                    onCheckedChange = viewModel::onTouchBoostChange,
                )
            }

            SectionCard(title = stringResource(R.string.tweak_section_display)) {
                TweakSlider(
                    label = stringResource(R.string.tweak_dpi),
                    description = stringResource(R.string.tweak_dpi_desc),
                    valueText = "${state.dpi} dpi",
                    value = state.dpi.toFloat(),
                    range = state.minDpi.toFloat()..state.maxDpi.toFloat(),
                    steps = ((state.maxDpi - state.minDpi) / 8).coerceAtLeast(1) - 1,
                    onValueChange = viewModel::onDpiChange,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = dpiInput,
                        onValueChange = { text ->
                            dpiInput = text.filter { it.isDigit() }
                            viewModel.onDpiTextChange(dpiInput)
                        },
                        label = { Text(stringResource(R.string.tweak_dpi_manual_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(
                            R.string.tweak_width_projected,
                            state.projectedWidthDp,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text = stringResource(R.string.tweak_dpi_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = viewModel::resetTweaks,
                    modifier = Modifier.weight(1f),
                    enabled = !state.applying,
                ) {
                    Text(stringResource(R.string.tweak_reset))
                }
                Button(
                    onClick = viewModel::applyTweaks,
                    modifier = Modifier.weight(1f),
                    enabled = !state.applying,
                ) {
                    if (state.applying) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(2.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(stringResource(R.string.tweak_apply))
                    }
                }
            }
        }
        SnackbarHost(hostState = snackbarHostState)
    }
}
