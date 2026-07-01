package com.aether.x.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aether.x.R
import com.aether.x.ui.settings.SettingsScreen
import com.aether.x.ui.tweak.TweakScreen

private enum class MainTab { TWEAK, SETTINGS }

@Composable
fun MainScreen(
    onViewGuideAgain: () -> Unit,
    onManageAccess: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(MainTab.TWEAK) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == MainTab.TWEAK,
                    onClick = { selectedTab = MainTab.TWEAK },
                    icon = { Icon(Icons.Filled.Tune, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_tweak)) },
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.SETTINGS,
                    onClick = { selectedTab = MainTab.SETTINGS },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_settings)) },
                )
            }
        },
    ) { padding ->
        when (selectedTab) {
            MainTab.TWEAK -> TweakScreen(modifier = Modifier, contentPadding = padding)
            MainTab.SETTINGS -> SettingsScreen(
                modifier = Modifier,
                contentPadding = padding,
                onViewGuideAgain = onViewGuideAgain,
                onManageAccess = onManageAccess,
            )
        }
    }
}
