package com.beradeep.aiyo.di

import android.content.Context
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
object DaoModule {
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