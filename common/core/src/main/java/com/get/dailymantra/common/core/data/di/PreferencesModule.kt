package com.get.dailymantra.common.core.data.di

import com.get.dailymantra.common.core.data.preferences.LocalUserPreferencesStore
import com.get.dailymantra.common.core.data.preferences.UserPreferencesStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesModule {

    @Binds
    @Singleton
    abstract fun bindUserPreferencesStore(
        impl: LocalUserPreferencesStore,
    ): UserPreferencesStore
}
