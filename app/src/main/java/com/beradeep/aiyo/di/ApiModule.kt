package com.beradeep.aiyo.di

import android.content.Context
import com.beradeep.aiyo.data.remote.ApiClientImpl
import com.beradeep.aiyo.data.remote.DataApiClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Provides
    @Singleton
    fun provideDataApiClient(@ApplicationContext context: Context): DataApiClient {
        return ApiClientImpl()
    }
}