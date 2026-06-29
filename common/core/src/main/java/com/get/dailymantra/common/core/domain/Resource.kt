package com.get.dailymantra.common.core.domain

/**
 * Represents the lifecycle state of an asynchronous data operation.
 *
 * [Resource] is the single wrapper type used from the data layer through to the UI layer.
 * Repositories return `Flow<Resource<T>>`, ViewModels expose `StateFlow<Resource<T>>`,
 * and Composables render each variant.
 *
 * @param T The type of data carried in the [Success] state.
 *
 * ### Usage
 *
 * ```kotlin
 * // ViewModel
 * private val _mantras = MutableStateFlow<Resource<List<Mantra>>>(Resource.Idle)
 * val mantras: StateFlow<Resource<List<Mantra>>> = _mantras.asStateFlow()
 *
 * fun loadMantras() {
 *     viewModelScope.launch {
 *         repository.getMantras().collect { _mantras.value = it }
 *     }
 * }
 *
 * // Composable
 * when (val state = mantras.collectAsStateWithLifecycle().value) {
 *     Resource.Idle    -> Unit
 *     Resource.Loading -> LoadingIndicator()
 *     is Resource.Success -> MantraList(state.data)
 *     is Resource.Error   -> ErrorScreen(
 *         message = state.error.toUserMessage(),
 *         onRetry = if (state.error.isRetryable) viewModel::loadMantras else null
 *     )
 * }
 * ```
 */
sealed class Resource<out T> {

    /** Initial state before any operation has been triggered. */
    data object Idle : Resource<Nothing>()

    /** An operation is currently in progress. */
    data object Loading : Resource<Nothing>()

    /**
     * The operation completed successfully.
     *
     * @property data The result produced by the operation.
     */
    data class Success<out T>(val data: T) : Resource<T>()

    /**
     * The operation failed.
     *
     * Use [AppError.isRetryable] to decide whether to offer a retry action.
     *
     * @property error Typed error describing what went wrong.
     */
    data class Error(val error: AppError) : Resource<Nothing>()
}
