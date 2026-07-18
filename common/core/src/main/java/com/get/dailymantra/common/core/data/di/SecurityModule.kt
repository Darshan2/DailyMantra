package com.get.dailymantra.common.core.data.di

import com.get.dailymantra.common.core.data.network.DefaultTokenProvider
import com.get.dailymantra.common.core.data.network.TokenProvider
import com.get.dailymantra.common.core.data.security.EncryptedTokenStore
import com.get.dailymantra.common.core.data.security.TokenStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindTokenStore(impl: EncryptedTokenStore): TokenStore

    @Binds
    abstract fun bindTokenProvider(impl: DefaultTokenProvider): TokenProvider
}
