package com.aether.cloud.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "aether_prefs")

/**
 * Lightweight wrapper around DataStore for simple app-level flags
 * (currently just whether the onboarding flow has been completed).
 */
class AppPreferences(private val context: Context) {

    companion object {
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    suspend fun isOnboardingDone(): Boolean {
        return context.dataStore.data.first()[KEY_ONBOARDING_DONE] ?: false
    }

    suspend fun setOnboardingDone(done: Boolean = true) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_DONE] = done
        }
    }
}
