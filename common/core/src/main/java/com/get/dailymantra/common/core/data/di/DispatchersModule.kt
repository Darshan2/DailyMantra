package com.get.dailymantra.common.core.data.di

import com.get.dailymantra.common.core.coroutines.CoroutineDispatchers
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @Provides
    @Singleton
    fun provideCoroutineDispatchers(): CoroutineDispatchers = object : CoroutineDispatchers {
        override val io      = Dispatchers.IO
        override val main    = Dispatchers.Main
        override val default = Dispatchers.Default
    }
}
