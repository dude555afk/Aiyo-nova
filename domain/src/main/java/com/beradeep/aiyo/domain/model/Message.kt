package com.beradeep.aiyo.domain.model

import java.util.UUID

enum class Role(val value: String) {
    User("user"),
    Assistant("assistant"),
    System("system")
}

data class Message(
    val id: UUID,
    val role: Role,
    val content: String
)
