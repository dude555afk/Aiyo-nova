package com.beradeep.aiyo.domain.model

data class Conversation(
    val id: java.util.UUID,
    val title: String,
    val selectedModel: Model,
    val isStarred: Boolean,
    val createdAt: Long,
    val lastUpdatedAt: Long
)