package com.haramshield.di

import android.content.Context
import com.haramshield.ml.GamblingDetector
import com.haramshield.ml.ImagePreprocessor
import com.haramshield.ml.ModelManager
import com.haramshield.ml.NSFWDetector
import com.haramshield.ml.ObjectDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MLModule {
    
    @Provides
    @Singleton
    fun provideModelManager(@ApplicationContext context: Context): ModelManager {
        return ModelManager(context).also { it.initialize() }
    }
    
    @Provides
    @Singleton
    fun provideImagePreprocessor(): ImagePreprocessor {
        return ImagePreprocessor()
    }
    
    @Provides
    @Singleton
    fun provideNSFWDetector(
        modelManager: ModelManager,
        imagePreprocessor: ImagePreprocessor
    ): NSFWDetector {
        return NSFWDetector(modelManager, imagePreprocessor)
    }
    
    @Provides
    @Singleton
    fun provideObjectDetector(
        @ApplicationContext context: Context,
        imagePreprocessor: ImagePreprocessor
    ): ObjectDetector {
        return ObjectDetector(context, imagePreprocessor)
    }
    
    @Provides
    @Singleton
    fun provideGamblingDetector(): GamblingDetector {
        return GamblingDetector()
    }
}
