/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.get.dailymantra.common.core.data.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * A sealed class representing the current lifecycle state of an asynchronous operation
 * or data stream.
 *
 * The type parameter [R] uses the 'out' keyword, making the class **covariant**. This
 * means if `Int` is a subtype of `Number`, then `Result<Int>` is a subtype of `Result<Number>`.
 *
 * @param R The type of the data expected to be held by the [Success] state.
 */
sealed class Result<out R> {
    /**
     * The **initial state** before any operation has been started or after a reset.
     * Often used to represent the default state of a data stream or view model property.
     */
    object Idle : Result<Nothing>()

    /**
     * Represents a **successful operation**, holding the resulting [data].
     *
     * @property data The payload returned upon successful completion.
     * @param T The type of the successful data.
     */
    data class Success<out T>(val data: T) : Result<T>()

    /**
     * Represents a **failed operation**, holding the [error] that occurred.
     *
     * @property error The [error] encountered during the operation, providing
     * details about the cause of the failure.
     */
    data class Error(val error: Throwable) : Result<Nothing>()

    /**
     * Represents the **ongoing state** when an operation is actively running (in-flight).
     * Useful for triggering progress indicators or skeleton loaders on the UI.
     */
    object Loading : Result<Nothing>()


    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Error -> "Error[exception=$error]"
            Loading -> "Loading"
            Idle -> "Idle"
        }
    }
}

/**
 * Executes a suspending function and wraps its result into a [Result] type.
 *
 * This extension provides a convenient way to safely call suspending functions and capture
 * both successful results and exceptions without manually writing try-catch blocks.
 *
 * - On success, the returned [Result] will be [Result.Success] containing the function’s value.
 * - On failure, the returned [Result] will be [Result.Error] containing the thrown exception.
 *
 * @param T The type of the result produced by the suspending function.
 * @return [Result.Success] if the function completes normally, or [Result.Error] if an exception is thrown.
 *
 * ### Usage example:
 *
 * ```kotlin
 * // Suppose you have a suspending function
 * suspend fun fetchUserFromApi(id: Int): User {
 *     // Simulate API call
 *     if (id <= 0) throw IllegalArgumentException("Invalid ID")
 *     return User(id, "Alice")
 * }
 *
 * // Using `asResult` to wrap the call
 * suspend fun loadUser(id: Int) {
 *     val result: Result<User> = { fetchUserFromApi(id) }.asResult()
 *
 *     when (result) {
 *         is Result.Success -> println("User loaded: ${result.data}")
 *         is Result.Error -> println("Error occurred: ${result.exception.message}")
 *     }
 * }
 * ```
 */
suspend fun <T> (suspend () -> T).asResult(): Result<T> {
    return try {
        val data = this.invoke() // 'this()' or 'this.invoke()' both work
        Result.Success(data)
    } catch (e: Exception) {
        Result.Error(e)
    }
}


/**
 * A safe wrapper function that executes a suspending API call and provides its state
 * as a [Flow] of [Result].
 *
 * This function handles the entire lifecycle of a suspend call, from the initial loading
 * state to a final success or error state. It is designed to be used in a Repository or
 * Data Source to consistently handle network calls and decouple error handling from the
 * calling code.
 *
 * The resulting [Flow] is a **cold stream** that emits states only when collected.
 *
 * @param T The type of data returned by the suspending call. The type is nullable to
 * accommodate APIs that may return null data.
 * @param call The suspending lambda function that performs the API call. It takes no
 * arguments and returns a nullable type `T?`.
 * @return A [Flow] that emits the different states of the API call:
 * - [Result.Loading]: The initial state, emitted immediately before the call begins.
 * - [Result.Success]: Emitted with the data `T?` if the call succeeds.
 * - [Result.Error]: Emitted with an `Exception` if the call fails.
 *
 * ### Usage example:
 * ```kotlin
 * suspend fun fetchUserFromApi(id: Int): User {
 *     if (id <= 0) throw IllegalArgumentException("Invalid ID")
 *     return User(id, "Alice")
 * }
 *
 * // Usage in ViewModel
 * val userFlow: Flow<Result<User>> = safeApiCall { fetchUserFromApi(1) }
 *
 * viewModelScope.launch {
 *     userFlow.collect { result ->
 *         when (result) {
 *             is Result.Loading -> showLoadingSpinner()
 *             is Result.Success -> showUser(result.data)
 *             is Result.Error -> showError(result.exception.message ?: "Unknown error")
 *         }
 *     }
 * }
 * ```
 */

fun <T> safeApiCall(call: suspend () -> T): Flow<Result<T>> {
    return flow {
        emit(Result.Loading)
        val res =  call.asResult()
        emit(res)
    }
}

inline fun <T, R> Result<T>.mapData(transform: (dataModel: T) -> R): Result<R> {
    return when(this) {
        is Result.Success -> Result.Success(transform(this.data))
        is Result.Error -> this
        is Result.Idle -> this
        is Result.Loading -> this
    }
}

inline fun <T, R> Result<List<T>>.mapListData(transform: (T) -> R): Result<List<R>> =
    mapData { list -> list.map(transform) }

/**
 * Maps the successful [Result] value emitted by this [Flow] from type [T] to [R],
 * while preserving the original [Result] state (such as [Result.Loading], [Result.Error], etc.).
 *
 * This is a convenience extension for transforming data inside a [Flow] of [Result]s,
 * without altering the surrounding [Result] status.
 *
 * Example:
 * ```
 * viewModelScope.launch {
 *     repository.userFlow()
 *         .mapData { user -> user.toUiModel() }
 *         .collect { result ->
 *             when (result) {
 *                 is Result.Success -> showUser(result.data)
 *                 is Result.Error -> showError(result.message)
 *             }
 *         }
 * }
 * ```
 *
 * @param transform A pure mapping function applied only to [Result.Success.data].
 * @return A [Flow] emitting [Result] of transformed data type [R].
 */
fun <T, R> Flow<Result<T>>.mapData(transform: (T) -> R): Flow<Result<R>> =
    map { it.mapData(transform) }


/**
 * Maps each element within a successful [Result] that contains a [List] of [T],
 * transforming it into a [List] of [R], while preserving the [Result] wrapper state.
 *
 * This is useful for adapting domain-layer lists to UI-layer models within a reactive data stream.
 *
 * Example:
 * ```
 * viewModelScope.launch {
 *     repository.productsFlow()
 *         .mapListData { product -> product.toUiModel() }
 *         .collect { result ->
 *             when (result) {
 *                 is Result.Success -> showProducts(result.data)
 *                 is Result.Error -> showError(result.message)
 *             }
 *         }
 * }
 * ```
 *
 * @param transform A pure mapping function applied to each item in [Result.Success.data].
 * @return A [Flow] emitting [Result] of transformed [List] type [R].
 */
fun <T, R> Flow<Result<List<T>>>.mapListData(transform: (T) -> R): Flow<Result<List<R>>> =
    map { it.mapListData(transform) }

/**
 * Safely executes a suspend API call and emits its result as a [Flow] of [Result].
 *
 * This function provides a consistent way to handle loading, success, and error states
 * when performing network or long-running operations. It wraps the given [call] inside a
 * `Flow` and automatically emits a [Result.Loading] state before executing it.
 *
 * Optionally, you can provide an [errorOnResult] predicate to interpret specific successful
 * results as errors (e.g., empty data, invalid response codes, etc.).
 *
 * @param T The type of data returned by the API call.
 * @param call A suspend lambda representing the API call to execute.
 * @param errorOnResult A predicate that returns `true` if the API result should be treated as an error.
 * @param errorOnResultMessage A custom message to include in the [Result.Error] when [errorOnResult] returns `true`.
 *
 * @return A [Flow] emitting:
 *  - [Result.Loading] before the call starts
 *  - [Result.Success] with the result if successful
 *  - [Result.Error] if an exception occurs or [errorOnResult] evaluates to `true`
 *
 * ### Example Usage
 *
 * ```kotlin
 * val userFlow = safeApiCall(
 *     call = { apiService.getUserDetails(userId) },
 *     errorOnResult = { user -> user.name.isEmpty() },
 *     errorOnResultMessage = "User data is invalid"
 * )
 *
 * userFlow.collect { result ->
 *     when (result) {
 *         is Result.Loading -> showLoading()
 *         is Result.Success -> showUser(result.data)
 *         is Result.Error -> showError(result.exception.message)
 *     }
 * }
 * ```
 *
 * @see Result
 */
fun <T> safeApiCall(
    call: suspend () -> T,
    errorOnResult: suspend (T) -> Boolean,
    errorOnResultMessage: String = ""
): Flow<Result<T>> {
    return flow {
        emit(Result.Loading)
        val res = call.asResult()

        if(res is Result.Success && errorOnResult(res.data)) {
            emit(Result.Error(Exception(errorOnResultMessage)))
        } else {
            emit(res)
        }
    }
}








