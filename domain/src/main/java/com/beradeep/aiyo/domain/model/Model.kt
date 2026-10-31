package com.beradeep.aiyo.domain.model

data class Model(
    val id: String,
    val ownedBy: String? = null,
    val createdAt: java.util.Date? = null
) {
    override fun equals(other: Any?): Boolean {
        return this.id == (other as Model).id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {
        val defaultModel = Model("anthropic/claude-sonnet-4.5")
    }
}