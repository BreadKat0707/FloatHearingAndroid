package cn.lemondrop.fhreborn.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fh_settings")

class SettingsRepository(private val context: Context) {

    private val dataStore = context.dataStore

    companion object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val FIRST_SCAN_DONE = booleanPreferencesKey("first_scan_done")
        val LYRIC_ALIGN_CENTER = booleanPreferencesKey("lyric_align_center")
    }

    val isOnboardingCompleted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[ONBOARDING_COMPLETED] ?: false
    }

    val isFirstScanDone: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[FIRST_SCAN_DONE] ?: false
    }

    val isLyricAlignCenter: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[LYRIC_ALIGN_CENTER] ?: false
    }

    suspend fun setLyricAlignCenter(center: Boolean) {
        dataStore.edit { prefs ->
            prefs[LYRIC_ALIGN_CENTER] = center
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setFirstScanDone(done: Boolean) {
        dataStore.edit { prefs ->
            prefs[FIRST_SCAN_DONE] = done
        }
    }
}
