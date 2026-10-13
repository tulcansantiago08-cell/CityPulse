package com.citypulse.di

import com.citypulse.BuildConfig
import com.citypulse.data.remote.ticketmaster.TicketmasterApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) {
                // Solo en debug, y con la clave de la API oculta en la URL registrada.
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC; redactQueryParams("apikey") })
            }
        }
        .build()

    @Provides
    @Singleton
    fun provideTicketmasterApi(client: OkHttpClient, json: Json): TicketmasterApi {
        val withKey = client.newBuilder()
            .addInterceptor { chain ->
                val request = chain.request()
                val url = request.url.newBuilder().addQueryParameter("apikey", BuildConfig.TM_API_KEY).build()
                chain.proceed(request.newBuilder().url(url).build())
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(TicketmasterApi.BASE_URL)
            .client(withKey)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TicketmasterApi::class.java)
    }
}
