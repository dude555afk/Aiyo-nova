package com.beradeep.aiyo.domain.repository

import com.beradeep.aiyo.domain.model.Model

interface ModelRepository {
    suspend fun getModels(): Result<List<Model>>
    fun getDefaultModel(): Model
    fun setDefaultModel(model: Model)
}