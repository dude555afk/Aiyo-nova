package com.beradeep.aiyo.domain.model

data class Model(
    val id: String,
    val ownedBy: String? = null,
    val createdAt: java.util.Date? = null
) {
    override fun equals(other: Any?): Boolean {
        return other is Model && this.id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {
        // OpenRouter routes to the best currently available free model.
        val defaultModel = Model("openrouter/free")
    }
}
