package com.citypulse.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.citypulse.data.fake.FakeEventRepository
import com.citypulse.data.fake.FakeLocationRepository
import com.citypulse.data.fake.InMemoryUserPreferencesRepository
import com.citypulse.data.local.DataStoreSurpriseRollsRepository
import com.citypulse.data.local.cityPulseDataStore
import com.citypulse.domain.repository.EventRepository
import com.citypulse.domain.repository.LocationRepository
import com.citypulse.domain.repository.SurpriseRollsRepository
import com.citypulse.domain.repository.UserPreferencesRepository
import com.citypulse.domain.time.Clock
import com.citypulse.domain.time.SystemClock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindEventRepository(impl: FakeEventRepository): EventRepository

    @Binds
    abstract fun bindUserPreferencesRepository(impl: InMemoryUserPreferencesRepository): UserPreferencesRepository

    @Binds
    abstract fun bindLocationRepository(impl: FakeLocationRepository): LocationRepository

    @Binds
    abstract fun bindSurpriseRollsRepository(impl: DataStoreSurpriseRollsRepository): SurpriseRollsRepository

    companion object {
        @Provides
        fun provideClock(): Clock = SystemClock()

        @Provides
        @Singleton
        fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            context.cityPulseDataStore
    }
}
