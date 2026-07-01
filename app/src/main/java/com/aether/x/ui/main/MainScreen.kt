package com.aether.x.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aether.x.R
import com.aether.x.ui.components.GameFab
import com.aether.x.ui.settings.SettingsScreen
import com.aether.x.ui.tweak.TweakScreen
import com.aether.x.ui.tweak.TweakViewModel

private enum class MainTab { TWEAK, SETTINGS }

@Composable
fun MainScreen(
    onViewGuideAgain: () -> Unit,
    onManageAccess: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(MainTab.TWEAK) }

    // ViewModel diangkat ke level ini supaya tombol mengambang (FAB) di sisi kanan
    // bisa langsung membuka game terdeteksi, terlepas dari posisi scroll di TweakScreen.
    val tweakViewModel: TweakViewModel = viewModel()
    val tweakState by tweakViewModel.state.collectAsStateWithLifecycle()
    val firstDetectedGame = tweakState.detectedGames.firstOrNull()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                tonalElevation = 0.dp,
            ) {
                NavigationBarItem(
                    selected = selectedTab == MainTab.TWEAK,
                    onClick = { selectedTab = MainTab.TWEAK },
                    icon = { Icon(Icons.Filled.Tune, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_tweak)) },
                    colors = aetherNavColors(),
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.SETTINGS,
                    onClick = { selectedTab = MainTab.SETTINGS },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_settings)) },
                    colors = aetherNavColors(),
                )
            }
        },
        floatingActionButton = {
            GameFab(
                gameName = firstDetectedGame?.displayName.orEmpty(),
                visible = selectedTab == MainTab.TWEAK && firstDetectedGame != null,
                onClick = {
                    firstDetectedGame?.let { tweakViewModel.launchGame(it.packageName) }
                },
            )
        },
    ) { padding ->
        when (selectedTab) {
            MainTab.TWEAK -> TweakScreen(
                modifier = Modifier,
                contentPadding = padding,
                viewModel = tweakViewModel,
            )
            MainTab.SETTINGS -> SettingsScreen(
                modifier = Modifier,
                contentPadding = padding,
                onViewGuideAgain = onViewGuideAgain,
                onManageAccess = onManageAccess,
            )
        }
    }
}

@Composable
private fun aetherNavColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.primary,
    selectedTextColor = MaterialTheme.colorScheme.primary,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
)
