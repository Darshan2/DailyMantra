package com.get.dailymantra.common.core.data.network.utils

import com.get.dailymantra.common.core.data.network.interceptors.UnauthenticatedException
import com.get.dailymantra.common.core.domain.AppError
import com.get.dailymantra.common.core.domain.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException

private suspend fun <T> safeCall(call: suspend () -> T): ApiResponse<T> {
    return try {
        ApiResponse.Success(call())
    } catch (e: UnauthenticatedException) {
        ApiResponse.Error(401, e.message)
    } catch (e: HttpException) {
        ApiResponse.Error(e.code(), e.message())
    } catch (e: IOException) {
        ApiResponse.Exception(e)
    } catch (e: Throwable) {
        ApiResponse.Exception(e)
    }
}

private fun ApiResponse.Error.toAppError(): AppError = when (code) {
    401 -> AppError.Auth(message)
    404 -> AppError.NotFound(message)
    else -> AppError.Server(code, message)
}

private fun <T> ApiResponse<T>.toResource(): Resource<T> = when (this) {
    is ApiResponse.Success -> Resource.Success(data)
    is ApiResponse.Error -> Resource.Error(toAppError())
    is ApiResponse.Exception -> Resource.Error(AppError.Network(throwable))
}

/**
 * Wraps a suspending Retrofit call in a cold [Flow] that emits [Resource.Loading] first,
 * then [Resource.Success] or [Resource.Error] depending on the outcome.
 *
 * HTTP errors are classified into typed [AppError] subtypes automatically:
 * - 401 → [AppError.Auth] (not retryable)
 * - 404 → [AppError.NotFound] (not retryable)
 * - other HTTP → [AppError.Server] (retryable)
 * - network / IO → [AppError.Network] (retryable)
 *
 * ### Usage
 *
 * ```kotlin
 * // Repository
 * fun getDailyMantra(): Flow<Resource<Mantra>> =
 *     safeApiCall { api.getDailyMantra() }
 *
 * // ViewModel
 * viewModelScope.launch {
 *     repository.getDailyMantra().collect { _state.value = it }
 * }
 * ```
 *
 * @param call The suspending lambda that performs the Retrofit call.
 * @return A cold [Flow] emitting [Resource.Loading] → [Resource.Success] or [Resource.Error].
 */
fun <T> safeApiCall(call: suspend () -> T): Flow<Resource<T>> = flow {
    emit(Resource.Loading)
    emit(safeCall(call).toResource())
}

/**
 * Variant of [safeApiCall] that treats an otherwise-successful response as an error when
 * [errorOnResult] returns `true`. Useful for business-level checks on the response body
 * (e.g., empty list, invalid token in response).
 *
 * ### Usage
 *
 * ```kotlin
 * fun getMantras(): Flow<Resource<List<Mantra>>> = safeApiCall(
 *     call = { api.getMantras() },
 *     errorOnResult = { it.isEmpty() },
 *     errorOnResultMessage = "No mantras available"
 * )
 * ```
 *
 * @param call The suspending lambda that performs the Retrofit call.
 * @param errorOnResult Returns `true` if the successful result should be treated as an error.
 * @param errorOnResultMessage Message passed to [AppError.Unknown] when [errorOnResult] is `true`.
 */
fun <T> safeApiCall(
    call: suspend () -> T,
    errorOnResult: suspend (T) -> Boolean,
    errorOnResultMessage: String = ""
): Flow<Resource<T>> = flow {
    emit(Resource.Loading)
    val response = safeCall(call)
    if (response is ApiResponse.Success && errorOnResult(response.data)) {
        emit(Resource.Error(AppError.Unknown(errorOnResultMessage)))
    } else {
        emit(response.toResource())
    }
}

/**
 * Transforms the data inside [Success], leaving all other states unchanged.
 *
 * ### Usage
 *
 * ```kotlin
 * val uiModel: Resource<MantraUiModel> = resource.mapData { it.toUiModel() }
 * ```
 */
inline fun <T, R> Resource<T>.mapData(transform: (T) -> R): Resource<R> = when (this) {
    is Resource.Success -> Resource.Success(transform(data))
    is Resource.Error -> this
    is Resource.Idle -> this
    is Resource.Loading -> this
}

/**
 * Transforms each item in a [Success] that wraps a [List], leaving all other states unchanged.
 *
 * ### Usage
 *
 * ```kotlin
 * val uiModels: Resource<List<MantraUiModel>> = resource.mapListData { it.toUiModel() }
 * ```
 */
inline fun <T, R> Resource<List<T>>.mapListData(transform: (T) -> R): Resource<List<R>> =
    mapData { list -> list.map(transform) }

/**
 * Maps the data inside each [Resource.Success] emitted by this [Flow].
 *
 * ### Usage
 *
 * ```kotlin
 * // Repository
 * fun getMantras(): Flow<Resource<List<MantraUiModel>>> =
 *     safeApiCall { api.getMantras() }
 *         .mapData { it.toUiModel() }
 * ```
 */
fun <T, R> Flow<Resource<T>>.mapData(transform: (T) -> R): Flow<Resource<R>> =
    map { it.mapData(transform) }

/**
 * Maps each item in the list inside [Resource.Success] emitted by this [Flow].
 *
 * ### Usage
 *
 * ```kotlin
 * // Repository
 * fun getMantras(): Flow<Resource<List<MantraUiModel>>> =
 *     safeApiCall { api.getMantras() }
 *         .mapListData { it.toUiModel() }
 * ```
 */
fun <T, R> Flow<Resource<List<T>>>.mapListData(transform: (T) -> R): Flow<Resource<List<R>>> =
    map { it.mapListData(transform) }
