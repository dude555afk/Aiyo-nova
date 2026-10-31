package com.beradeep.aiyo.di

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.beradeep.aiyo.data.local.db.AiyoDatabase
import com.beradeep.aiyo.data.local.db.dao.ConversationDao
import com.beradeep.aiyo.data.local.db.dao.MessageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DbModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AiyoDatabase {
        return AiyoDatabase.create(context)
    }

    @Provides
    @Singleton
    fun provideConversationDao(database: AiyoDatabase): ConversationDao {
        return database.conversationDao()
    }

    @Provides
    @Singleton
    fun provideMessageDao(database: AiyoDatabase): MessageDao {
        return database.messageDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Provides
    @Singleton
    fun provideApiClientImpl(): com.beradeep.aiyo.data.remote.ApiClientImpl {
        return com.beradeep.aiyo.data.remote.ApiClientImpl()
    }
}