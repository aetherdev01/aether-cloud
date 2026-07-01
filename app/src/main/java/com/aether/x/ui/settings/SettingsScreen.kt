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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aether.x.BuildConfig
import com.aether.x.R
import com.aether.x.core.permission.PrivilegeManager
import com.aether.x.data.DarkModePref
import com.aether.x.ui.components.SectionCard
import com.aether.x.ui.components.StatusPill
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

        SectionCard(title = stringResource(R.string.settings_section_access)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "Shizuku", style = MaterialTheme.typography.bodyLarge)
                StatusPill(
                    text = if (privilegeStatus.shizukuGranted) {
                        stringResource(R.string.setup_status_granted)
                    } else {
                        stringResource(R.string.setup_status_not_granted)
                    },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "Root", style = MaterialTheme.typography.bodyLarge)
                StatusPill(
                    text = if (privilegeStatus.rootGranted) {
                        stringResource(R.string.setup_status_granted)
                    } else {
                        stringResource(R.string.setup_status_not_granted)
                    },
                )
            }
            Button(onClick = onManageAccess, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.setup_title))
            }
        }

        SectionCard(title = stringResource(R.string.settings_section_other)) {
            Button(onClick = onViewGuideAgain, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_view_guide))
            }
            Column {
                Text(text = stringResource(R.string.settings_about), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
