package com.aether.x

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aether.x.data.AetherXPreferences
import com.aether.x.data.AppPreferences
import com.aether.x.data.DarkModePref
import com.aether.x.ui.main.MainScreen
import com.aether.x.ui.navigation.AetherXRoutes
import com.aether.x.ui.onboarding.GuideScreen
import com.aether.x.ui.onboarding.PermissionSetupScreen
import com.aether.x.ui.onboarding.SplashScreen
import com.aether.x.ui.theme.AetherXTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Splash tetap tampil sampai status onboarding selesai dibaca dari DataStore,
        // supaya tidak ada "flash" layar kosong sebelum tujuan navigasi ditentukan.
        var keepSplashScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }

        setContent {
            val preferences = remember { AetherXPreferences(applicationContext) }
            val nullablePrefsFlow: Flow<AppPreferences?> = preferences.preferences
            val appPrefs by nullablePrefsFlow.collectAsStateWithLifecycle(initialValue = null)

            LaunchedEffect(appPrefs) {
                if (appPrefs != null) keepSplashScreen = false
            }

            appPrefs?.let { prefsValue ->
                val darkTheme = when (prefsValue.darkModePref) {
                    DarkModePref.SYSTEM -> isSystemInDarkTheme()
                    DarkModePref.LIGHT -> false
                    DarkModePref.DARK -> true
                }
                AetherXTheme(
                    darkTheme = darkTheme,
                    useDynamicColor = prefsValue.dynamicColorEnabled,
                ) {
                    AetherXRoot(
                        onboardingCompleted = prefsValue.onboardingCompleted,
                        preferences = preferences,
                    )
                }
            }
        }
    }
}

@Composable
private fun AetherXRoot(
    onboardingCompleted: Boolean,
    preferences: AetherXPreferences,
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val startDestination = if (onboardingCompleted) AetherXRoutes.MAIN else AetherXRoutes.SETUP_ONBOARDING

    NavHost(navController = navController, startDestination = startDestination) {
        composable(AetherXRoutes.SETUP_ONBOARDING) {
            SplashScreen(
                onDone = { navController.navigate(AetherXRoutes.GUIDE_ONBOARDING) },
            )
        }
        composable(AetherXRoutes.GUIDE_ONBOARDING) {
            GuideScreen(
                onFinish = {
                    scope.launch { preferences.setOnboardingCompleted(true) }
                    navController.navigate(AetherXRoutes.MAIN) {
                        popUpTo(AetherXRoutes.SETUP_ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(AetherXRoutes.MAIN) {
            MainScreen(
                onViewGuideAgain = { navController.navigate(AetherXRoutes.GUIDE_REVISIT) },
                onManageAccess = { navController.navigate(AetherXRoutes.MANAGE_ACCESS) },
            )
        }
        composable(AetherXRoutes.MANAGE_ACCESS) {
            PermissionSetupScreen(
                onContinue = { navController.popBackStack() },
                requireAccessToContinue = false,
            )
        }
        composable(AetherXRoutes.GUIDE_REVISIT) {
            GuideScreen(onFinish = { navController.popBackStack() })
        }
    }
}
