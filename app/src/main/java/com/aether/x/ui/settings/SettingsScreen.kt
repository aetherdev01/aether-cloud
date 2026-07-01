package com.aether.x.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
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
        Text(text = stringResource(R.string.nav_settings), style = MaterialTheme.typography.headlineSmall)

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
            Column {
                Text(text = stringResource(R.string.settings_maintainer), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(R.string.settings_maintainer_name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.settings_maintainer_handle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = stringResource(R.string.settings_about), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onViewGuideAgain, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_view_guide))
            }
            OutlinedButton(onClick = onManageAccess, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_manage_access))
            }
        }
    }
}
