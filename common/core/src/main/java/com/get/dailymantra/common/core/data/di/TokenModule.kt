package com.get.dailymantra.common.core.data.di

import com.get.dailymantra.common.core.data.network.DefaultTokenProvider
import com.get.dailymantra.common.core.data.network.TokenProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TokenModule {

    @Binds
    @Singleton
    abstract fun bindTokenProvider(impl: DefaultTokenProvider): TokenProvider
}
