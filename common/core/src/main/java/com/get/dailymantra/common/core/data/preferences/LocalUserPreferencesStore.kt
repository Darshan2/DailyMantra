package com.get.dailymantra.common.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// Must be top-level: the delegate guarantees one DataStore instance per process for this name.
private val Context.userPreferencesDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "daily_mantra_user_preferences")

class LocalUserPreferencesStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : UserPreferencesStore {

    private object Keys {
        val USER_ID   = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
    }

    override val userPreferences: Flow<UserPreferences> =
        context.userPreferencesDataStore.data.map { prefs ->
            UserPreferences(
                userId   = prefs[Keys.USER_ID],
                userName = prefs[Keys.USER_NAME],
            )
        }

    override suspend fun saveUserId(userId: String) {
        context.userPreferencesDataStore.edit { it[Keys.USER_ID] = userId }
    }

    override suspend fun saveUserName(userName: String) {
        context.userPreferencesDataStore.edit { it[Keys.USER_NAME] = userName }
    }

    override suspend fun clearAll() {
        context.userPreferencesDataStore.edit { it.clear() }
    }
}
