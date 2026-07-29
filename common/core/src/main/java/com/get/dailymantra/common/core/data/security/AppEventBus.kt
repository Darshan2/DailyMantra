package com.get.dailymantra.common.core.data.security

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AppEvent {
    /** Emitted when the refresh token is missing/rejected and re-authentication is required. */
    data object SessionExpired : AppEvent
}


@Singleton
class AppEventBus @Inject constructor() {

    private val _events = MutableSharedFlow<AppEvent>(
        replay = 0,
        extraBufferCapacity = 1,
    )
    val events: SharedFlow<AppEvent> = _events

    suspend fun notifySessionExpired() = _events.emit(AppEvent.SessionExpired)
}
