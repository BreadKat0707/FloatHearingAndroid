package cn.lemondrop.fhreborn.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
        val HIDDEN_FOLDERS = stringSetPreferencesKey("hidden_folders")
        val SORT_FIELD = stringPreferencesKey("sort_field")
        val SORT_ORDER = stringPreferencesKey("sort_order")
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

    val hiddenFolders: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[HIDDEN_FOLDERS] ?: emptySet()
    }

    suspend fun addHiddenFolder(path: String) {
        dataStore.edit { prefs ->
            val current = prefs[HIDDEN_FOLDERS] ?: emptySet()
            prefs[HIDDEN_FOLDERS] = current + path
        }
    }

    suspend fun removeHiddenFolder(path: String) {
        dataStore.edit { prefs ->
            val current = prefs[HIDDEN_FOLDERS] ?: emptySet()
            prefs[HIDDEN_FOLDERS] = current - path
        }
    }

    val sortField: Flow<String?> = dataStore.data.map { prefs ->
        prefs[SORT_FIELD]
    }

    val sortOrder: Flow<String?> = dataStore.data.map { prefs ->
        prefs[SORT_ORDER]
    }

    suspend fun setSortField(fieldName: String) {
        dataStore.edit { prefs -> prefs[SORT_FIELD] = fieldName }
    }

    suspend fun setSortOrder(orderName: String) {
        dataStore.edit { prefs -> prefs[SORT_ORDER] = orderName }
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
