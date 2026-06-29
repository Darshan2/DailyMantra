package com.get.dailymantra.common.core.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * An extension function on [Flow] that converts a cold [Flow] into a hot [StateFlow].
 *
 * This function provides a convenient way to create a [StateFlow] that is tied to a specific
 * [CoroutineScope] and lifecycle.
 *
 * The resulting [StateFlow] is configured to start collecting from the upstream [Flow]
 * when the first subscriber appears and will keep the flow active for 5 seconds after the
 * last subscriber disappears, which is useful for surviving configuration changes without
 * restarting the data fetch.
 *
 * @param T The type of data emitted by the upstream [Flow].
 * @param coroutineScope The [CoroutineScope] in which the sharing coroutine will run.
 * This is typically a lifecycle-aware scope like a `viewModelScope`.
 * @return A [StateFlow] that emits the latest value from the upstream [Flow]. The initial
 * value of the [StateFlow] is `null` until the first value is emitted by the source [Flow].
 */
fun <T> Flow<T>.toStateFlow(coroutineScope: CoroutineScope, initialValue: T? = null): StateFlow<T?> {
    return this.stateIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L),
        initialValue = initialValue
    )
}
