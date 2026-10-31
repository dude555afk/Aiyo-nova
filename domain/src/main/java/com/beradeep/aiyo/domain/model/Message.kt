package com.beradeep.aiyo.domain.model

import java.util.UUID

sealed class Role {
    data object User : Role()
    data object Assistant : Role()
    data object System : Role()
}

data class Message(
    val id: UUID,
    val role: Role,
    val content: String
)