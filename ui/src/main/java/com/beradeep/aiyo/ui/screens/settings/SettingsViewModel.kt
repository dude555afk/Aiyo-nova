package com.beradeep.aiyo.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beradeep.aiyo.domain.ApiClient
import com.beradeep.aiyo.domain.model.Model
import com.beradeep.aiyo.domain.model.ThemeType
import com.beradeep.aiyo.domain.repository.ModelRepository
import com.beradeep.aiyo.domain.repository.SettingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

open class SettingsViewModel(
    private val modelRepository: ModelRepository,
    private val settingsRepository: SettingRepository,
    private val apiClient: ApiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState.Default)

    val uiState = _uiState
        .onStart { loadInitialState() }
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            SettingsUiState.Default
        )

    fun onUiEvent(settingsUiEvent: SettingsUiEvent) {
        when (settingsUiEvent) {
            is SettingsUiEvent.OnModelSelected -> selectModel(settingsUiEvent.model)
            SettingsUiEvent.OnFetchModels -> fetchModels()
            SettingsUiEvent.OnShowModelSelectionSheet -> showModelSelectionSheet()
            SettingsUiEvent.OnDismissModelSelectionSheet -> dismissModelSelectionSheet()
            is SettingsUiEvent.OnUpdateThemeType -> setThemeType(settingsUiEvent.themeType)
            is SettingsUiEvent.OnUpdateRequestFontSize -> setRequestFontSize(settingsUiEvent.fontSize)
            is SettingsUiEvent.OnUpdateResponseFontSize -> setResponseFontSize(settingsUiEvent.fontSize)
        }
    }

    private fun loadInitialState() {
        loadThemeType()
        loadFontSizes()
        loadDefaultModel()
        fetchModels()
    }

    private fun loadThemeType() {
        viewModelScope.launch {
            _uiState.update { it.copy(themeType = settingsRepository.getThemeType()) }
        }
    }

    private fun loadFontSizes() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    requestFontSize = settingsRepository.getRequestFontSize(),
                    responseFontSize = settingsRepository.getResponseFontSize()
                )
            }
        }
    }

    private fun loadDefaultModel() {
        viewModelScope.launch {
            val savedModel = modelRepository.getDefaultModel()
            _uiState.update { it.copy(selectedModel = savedModel) }
        }
    }

    private fun setThemeType(themeType: ThemeType) {
        viewModelScope.launch {
            settingsRepository.setThemeType(themeType)
            _uiState.update { it.copy(themeType = themeType) }
        }
    }

    private fun setRequestFontSize(fontSize: Int) {
        viewModelScope.launch {
            settingsRepository.setRequestFontSize(fontSize)
            _uiState.update { it.copy(requestFontSize = settingsRepository.getRequestFontSize()) }
        }
    }

    private fun setResponseFontSize(fontSize: Int) {
        viewModelScope.launch {
            settingsRepository.setResponseFontSize(fontSize)
            _uiState.update { it.copy(responseFontSize = settingsRepository.getResponseFontSize()) }
        }
    }

    private fun fetchModels() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isFetchingModels = true) }
            modelRepository
                .getModels()
                .onSuccess { models ->
                    _uiState.update { it.copy(models = models) }
                    val currentSelected = _uiState.value.selectedModel
                    val newSelected = if (!models.contains(currentSelected)) {
                        models.firstOrNull() ?: currentSelected
                    } else {
                        currentSelected
                    }
                    modelRepository.setDefaultModel(newSelected)
                    loadDefaultModel()
                }
            _uiState.update { it.copy(isFetchingModels = false) }
        }
    }

    private fun selectModel(model: Model) {
        _uiState.update { it.copy(selectedModel = model) }
        modelRepository.setDefaultModel(model)
        dismissModelSelectionSheet()
    }

    private fun showModelSelectionSheet() {
        _uiState.update { it.copy(showModelSelectionSheet = true) }
    }

    private fun dismissModelSelectionSheet() {
        _uiState.update { it.copy(showModelSelectionSheet = false) }
    }
