package com.beradeep.aiyo.ui.screens.settings

import com.beradeep.aiyo.domain.model.Model
import com.beradeep.aiyo.domain.model.ThemeType
import com.beradeep.aiyo.domain.repository.SettingRepository

data class SettingsUiState(
    val models: List<Model>,
    val selectedModel: Model,
    val isFetchingModels: Boolean,
    val showModelSelectionSheet: Boolean,
    val themeType: ThemeType,
    val requestFontSize: Int,
    val responseFontSize: Int
) {
    companion object {
        val defaultModel = Model.defaultModel
        val Default = SettingsUiState(
            models = listOf(defaultModel),
            selectedModel = defaultModel,
            isFetchingModels = false,
            showModelSelectionSheet = false,
            themeType = ThemeType.System,
            requestFontSize = SettingRepository.DEFAULT_REQUEST_FONT_SIZE,
            responseFontSize = SettingRepository.DEFAULT_RESPONSE_FONT_SIZE
        )
    }
}