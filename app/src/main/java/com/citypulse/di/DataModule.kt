package com.citypulse.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.citypulse.data.city.StaticCityRepository
import com.citypulse.data.fake.FakeEventRepository
import com.citypulse.data.local.DataStoreSurpriseRollsRepository
import com.citypulse.data.local.DataStoreUserPreferencesRepository
import com.citypulse.data.location.AndroidLocationRepository
import com.citypulse.data.local.RoomSavedEventsRepository
import com.citypulse.data.local.db.CityPulseDatabase
import com.citypulse.data.local.db.SavedEventDao
import com.citypulse.data.network.AndroidConnectivityRepository
import com.citypulse.data.local.cityPulseDataStore
import com.citypulse.domain.repository.CityRepository
import com.citypulse.domain.repository.ConnectivityRepository
import com.citypulse.domain.repository.EventRepository
import com.citypulse.domain.repository.LocationRepository
import com.citypulse.domain.repository.SavedEventsRepository
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
    abstract fun bindUserPreferencesRepository(impl: DataStoreUserPreferencesRepository): UserPreferencesRepository

    @Binds
    abstract fun bindLocationRepository(impl: AndroidLocationRepository): LocationRepository

    @Binds
    abstract fun bindCityRepository(impl: StaticCityRepository): CityRepository

    @Binds
    abstract fun bindSavedEventsRepository(impl: RoomSavedEventsRepository): SavedEventsRepository

    @Binds
    abstract fun bindConnectivityRepository(impl: AndroidConnectivityRepository): ConnectivityRepository

    @Binds
    abstract fun bindSurpriseRollsRepository(impl: DataStoreSurpriseRollsRepository): SurpriseRollsRepository

    companion object {
        @Provides
        fun provideClock(): Clock = SystemClock()

        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): CityPulseDatabase =
            Room.databaseBuilder(context, CityPulseDatabase::class.java, "citypulse.db")
                .addMigrations(CityPulseDatabase.MIGRATION_1_2)
                .build()

        @Provides
        fun provideSavedEventDao(db: CityPulseDatabase): SavedEventDao = db.savedEventDao()

        @Provides
        @Singleton
        fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            context.cityPulseDataStore
    }
}
