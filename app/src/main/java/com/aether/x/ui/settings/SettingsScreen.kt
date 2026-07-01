package com.aether.x.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aether.x.BuildConfig
import com.aether.x.R
import com.aether.x.core.permission.PrivilegeManager
import com.aether.x.data.DarkModePref
import com.aether.x.data.TemperatureUnit
import com.aether.x.ui.components.SectionCard
import com.aether.x.ui.components.TweakSwitch

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onViewGuideAgain: () -> Unit,
    onManageAccess: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(),
) {
    val prefs by viewModel.state.collectAsStateWithLifecycle()
    val privilegeStatus by PrivilegeManager.status.collectAsStateWithLifecycle()
    var dragModeActive by remember { mutableStateOf(false) }
    var overlayGranted by remember { mutableStateOf(viewModel.canDrawOverlays()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlayGranted = viewModel.canDrawOverlays()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.nav_settings),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        SectionCard(title = stringResource(R.string.settings_section_appearance)) {
            TweakSwitch(
                label = stringResource(R.string.settings_dynamic_color),
                description = stringResource(R.string.settings_dynamic_color_desc),
                checked = prefs.dynamicColorEnabled,
                onCheckedChange = viewModel::setDynamicColorEnabled,
            )

            Column {
                Text(text = stringResource(R.string.settings_dark_mode), style = MaterialTheme.typography.bodyLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    val options = listOf(
                        DarkModePref.SYSTEM to stringResource(R.string.settings_dark_mode_system),
                        DarkModePref.LIGHT to stringResource(R.string.settings_dark_mode_light),
                        DarkModePref.DARK to stringResource(R.string.settings_dark_mode_dark),
                    )
                    options.forEachIndexed { index, (pref, label) ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                            selected = prefs.darkModePref == pref,
                            onClick = { viewModel.setDarkModePref(pref) },
                        ) {
                            Text(label)
                        }
                    }
                }
            }
        }

        SectionCard(title = stringResource(R.string.settings_section_general)) {
            Column {
                Text(
                    text = stringResource(R.string.settings_temperature_unit_label),
                    style = MaterialTheme.typography.bodyLarge,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    val options = listOf(
                        TemperatureUnit.CELSIUS to stringResource(R.string.settings_temperature_unit_celsius),
                        TemperatureUnit.FAHRENHEIT to stringResource(R.string.settings_temperature_unit_fahrenheit),
                    )
                    options.forEachIndexed { index, (unit, label) ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                            selected = prefs.temperatureUnit == unit,
                            onClick = { viewModel.setTemperatureUnit(unit) },
                        ) {
                            Text(label)
                        }
                    }
                }
            }
        }

        SectionCard(title = stringResource(R.string.settings_section_crosshair)) {
            CrosshairSettingsSection(
                enabled = prefs.crosshairEnabled,
                style = prefs.crosshairStyle,
                colorArgb = prefs.crosshairColor,
                sizeDp = prefs.crosshairSize,
                thicknessDp = prefs.crosshairThickness,
                opacityPercent = prefs.crosshairOpacity,
                overlayPermissionGranted = overlayGranted,
                dragModeActive = dragModeActive,
                onEnabledChange = viewModel::setCrosshairEnabled,
                onRequestOverlayPermission = viewModel::openOverlayPermissionSettings,
                onStyleChange = viewModel::setCrosshairStyle,
                onColorChange = viewModel::setCrosshairColor,
                onSizeChange = viewModel::setCrosshairSize,
                onThicknessChange = viewModel::setCrosshairThickness,
                onOpacityChange = viewModel::setCrosshairOpacity,
                onToggleDragMode = { active ->
                    dragModeActive = active
                    viewModel.setDragMode(active)
                },
                onResetPosition = viewModel::resetCrosshairPosition,
            )
        }

        SectionCard(title = stringResource(R.string.settings_section_fps_monitor)) {
            FpsMonitorSettingsSection(
                enabled = prefs.fpsMonitorEnabled,
                style = prefs.fpsMonitorStyle,
                overlayPermissionGranted = overlayGranted,
                hasShellAccess = privilegeStatus.hasAccess,
                onEnabledChange = viewModel::setFpsMonitorEnabled,
                onRequestOverlayPermission = viewModel::openOverlayPermissionSettings,
                onStyleChange = viewModel::setFpsMonitorStyle,
            )
        }

        SectionCard(title = stringResource(R.string.settings_section_about)) {
            AboutSection(versionName = BuildConfig.VERSION_NAME)
        }
    }
}

/**
 * Kartu "Tentang" yang lebih profesional: identitas app (nama + versi) sebagai
 * baris utama dengan lencana versi, lalu info maintainer di baris kedua yang
 * lebih ringkas — bukan lagi tumpukan Text kaku baris demi baris.
 */
@Composable
private fun AboutSection(versionName: String) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.settings_about),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.settings_about_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_version, versionName),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = stringResource(R.string.settings_maintainer_name),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            )
            Column {
                Text(
                    text = stringResource(R.string.settings_maintainer_name),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.settings_maintainer_handle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
