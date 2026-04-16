package com.theo.forest.di

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.HarmBlockThreshold
import com.theo.forest.Protected
import com.theo.forest.data.repository.ApiRepository
import com.theo.forest.ml.DiseaseDetectionModal
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
        return Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(
                modelName = "gemini-2.5-flash-lite",
            )
    }

    @Provides
    @Singleton
    fun provideRepository(generativeModel: com.google.firebase.ai.GenerativeModel): ApiRepository {
        return ApiRepository(generativeModel)
    }

}
