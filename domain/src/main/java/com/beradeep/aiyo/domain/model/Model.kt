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
        // Kilo's virtual router selects an available free model for each session.
        val defaultModel = Model("kilo-auto/free")
    }
}
