package com.citypulse.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.citypulse.data.local.DataStoreSurpriseRollsRepository
import com.citypulse.data.local.cityPulseDataStore
import com.citypulse.domain.repository.SurpriseRollsRepository
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
    abstract fun bindSurpriseRollsRepository(impl: DataStoreSurpriseRollsRepository): SurpriseRollsRepository

    companion object {
        @Provides
        @Singleton
        fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            context.cityPulseDataStore
    }
}
