package com.get.dailymantra.common.core.data.di

import android.content.Context
import com.get.dailymantra.common.core.BuildConfig
import com.get.dailymantra.common.core.data.network.AppMetadataProvider
import com.get.dailymantra.common.core.data.network.AuthApi
import com.get.dailymantra.common.core.data.network.DefaultAppMetadataProvider
import com.get.dailymantra.common.core.data.network.interceptors.HeaderAppMetaDataInterceptor
import com.get.dailymantra.common.core.data.network.interceptors.HeaderAuthInterceptor
import com.get.dailymantra.common.core.data.network.interceptors.IdempotencyInterceptor
import com.get.dailymantra.common.core.data.network.interceptors.RateLimitInterceptor
import com.get.dailymantra.common.core.data.network.interceptors.RetryInterceptor
import com.get.dailymantra.common.core.data.network.interceptors.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

/** Marks the dependency graph used only for the token-refresh call, kept isolated from [TokenAuthenticator]. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RefreshClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAppMetadataProvider(
        @ApplicationContext context: Context,
    ): AppMetadataProvider = DefaultAppMetadataProvider(context)

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideJsonConverterFactory(json: Json): Converter.Factory =
        json.asConverterFactory("application/json; charset=UTF8".toMediaType())


    @Provides
    @Singleton
    fun provideOkHttpClient(
        headerAppMetaDataInterceptor: HeaderAppMetaDataInterceptor,
        headerAuthInterceptor: HeaderAuthInterceptor,
        idempotencyInterceptor: IdempotencyInterceptor,
        retryInterceptor: RetryInterceptor,
        rateLimitInterceptor: RateLimitInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .authenticator(tokenAuthenticator)
            .addInterceptor(headerAppMetaDataInterceptor)
            .addInterceptor(headerAuthInterceptor)
            .addInterceptor(idempotencyInterceptor)
            .addInterceptor(rateLimitInterceptor)
            .addInterceptor(retryInterceptor)
            .build()


    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, converterFactory: Converter.Factory): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(converterFactory)
            .build()

    /**
     * Deliberately excludes [TokenAuthenticator]: it depends on [AuthApi], so wiring the
     * authenticator onto this client would form a cycle (and let a failed refresh call
     * recursively trigger another refresh).
     */
    @Provides
    @Singleton
    @RefreshClient
    fun provideRefreshOkHttpClient(
        headerAppMetaDataInterceptor: HeaderAppMetaDataInterceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(headerAppMetaDataInterceptor)
            .build()

    @Provides
    @Singleton
    @RefreshClient
    fun provideRefreshRetrofit(
        @RefreshClient okHttpClient: OkHttpClient,
        converterFactory: Converter.Factory,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(converterFactory)
            .build()

    @Provides
    @Singleton
    fun provideAuthApi(@RefreshClient retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)
}
