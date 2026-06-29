package com.get.dailymantra.common.core.data.network

/**
 * Marks a Retrofit API function as not requiring an Authorization header.
 *
 * By default, [HeaderInterceptor] attaches a `Bearer` token to every request.
 * Annotate a function with [NoAuthorization] to opt out — the interceptor will
 * skip token injection entirely for that endpoint.
 *
 * ### Usage
 * ```kotlin
 * interface AuthApi {
 *
 *     @NoAuthorization
 *     @POST("auth/login")
 *     suspend fun login(@Body body: LoginRequest): LoginResponse
 *
 *     @NoAuthorization
 *     @POST("auth/register")
 *     suspend fun register(@Body body: RegisterRequest): RegisterResponse
 *
 *     // No annotation — Authorization header is added automatically
 *     @GET("user/profile")
 *     suspend fun getProfile(): Profile
 * }
 * ```
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class NoAuthorization