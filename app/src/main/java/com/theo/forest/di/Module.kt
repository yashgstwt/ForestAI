package com.theo.forest.di

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.theo.forest.Protected
import com.theo.forest.data.remote.WeatherApiService
import com.theo.forest.data.repository.ApiRepository
import com.theo.forest.ml.DiseaseDetectionModal
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.auth.Auth
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object Module {

    @Singleton
    @Provides
    fun provideMlModal(@ApplicationContext context: Context): DiseaseDetectionModal {
        return DiseaseDetectionModal(context)
    }

    @Provides
    @Singleton
    fun provideGenerativeModel(): com.google.firebase.ai.GenerativeModel {
        return Firebase.ai.generativeModel(
            modelName = "gemini-2.5-flash-lite",
        )
    }
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = Protected.SUPABASE_URL,
            supabaseKey = Protected.SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
            install(Storage)
            install(Auth)
        }
    }

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://weather.visualcrossing.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideWeatherApiService(retrofit: Retrofit): WeatherApiService {
        return retrofit.create(WeatherApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideRepository(
        // Use the EXACT same type as provided above
        generativeModel: com.google.firebase.ai.GenerativeModel,
        weatherApiService: WeatherApiService,
        supabaseClient: SupabaseClient
    ): ApiRepository {
        return ApiRepository(generativeModel, weatherApiService, supabaseClient)
    }

}
