package com.get.dailymantra.common.core.data.preferences

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeUserPreferencesStore : UserPreferencesStore {

    private val _state = MutableStateFlow(UserPreferences())
    override val userPreferences: Flow<UserPreferences> = _state.asStateFlow()

    override suspend fun saveUserId(userId: String) {
        _state.update { it.copy(userId = userId) }
    }

    override suspend fun saveUserName(userName: String) {
        _state.update { it.copy(userName = userName) }
    }

    override suspend fun clearAll() {
        _state.value = UserPreferences()
    }
}
