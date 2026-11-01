package com.beradeep.aiyo.domain.repository

import com.beradeep.aiyo.domain.model.Conversation
import com.beradeep.aiyo.domain.model.Message
import com.beradeep.aiyo.domain.model.Model
import com.beradeep.aiyo.domain.model.Reason
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun getCompletion(model: Model, message: Message): Result<String?>
    suspend fun getChatCompletion(model: Model, messages: List<Message>): Result<String?>
    suspend fun getChatCompletionFlow(
        model: Model,
        messages: List<Message>,
        reason: Reason = Reason.None,
        webSearch: Boolean = false
    ): Result<Flow<String>>
    suspend fun getConversations(): List<Conversation>
    fun getConversationsFlow(): Flow<List<Conversation>>
    suspend fun createConversation(title: String, model: Model = Model.defaultModel, isStarred: Boolean = false): Conversation
    suspend fun updateConversation(conversation: Conversation)
    suspend fun deleteConversation(id: java.util.UUID)
    suspend fun getMessages(conversationId: java.util.UUID): List<Message>
    suspend fun insertMessage(message: Message, conversationId: java.util.UUID)
    suspend fun insertMessages(messages: List<Message>, conversationId: java.util.UUID)
    suspend fun deleteMessages(conversationId: java.util.UUID)
}
