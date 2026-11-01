package com.beradeep.aiyo.domain.model

/**
 * A document or image selected by the user. URI permissions are persisted by the picker,
 * while the actual file bytes are read only when a request is sent.
 */
data class MessageAttachment(
    val uri: String,
    val name: String,
    val mimeType: String
)
