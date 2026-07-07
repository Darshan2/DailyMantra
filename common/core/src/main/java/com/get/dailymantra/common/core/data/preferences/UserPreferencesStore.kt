package com.get.dailymantra.common.core.data.preferences

import kotlinx.coroutines.flow.Flow

interface UserPreferencesStore {
    val userPreferences: Flow<UserPreferences>
    suspend fun saveUserId(userId: String)
    suspend fun saveUserName(userName: String)
    suspend fun clearAll()
}
