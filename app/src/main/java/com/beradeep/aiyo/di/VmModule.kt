package com.beradeep.aiyo.di

import com.beradeep.aiyo.domain.ApiClient
import com.beradeep.aiyo.domain.repository.ChatRepository
import com.beradeep.aiyo.domain.repository.ModelRepository
import com.beradeep.aiyo.domain.repository.SettingRepository
import com.beradeep.aiyo.ui.screens.chat.ChatViewModel
import com.beradeep.aiyo.ui.screens.settings.SettingsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChatViewModelImpl
@Inject
constructor(
    chatRepository: ChatRepository,
    modelRepository: ModelRepository,
    settingRepository: SettingRepository,
    apiClient: ApiClient
) : ChatViewModel(
    chatRepository = chatRepository,
    modelRepository = modelRepository,
    settingRepository = settingRepository,
    apiClient = apiClient
)

@HiltViewModel
class SettingsViewModelImpl
@Inject
constructor(
    apiClient: ApiClient,
    modelRepository: ModelRepository,
    settingRepository: SettingRepository
) : SettingsViewModel(
    modelRepository = modelRepository,
    settingsRepository = settingRepository,
    apiClient = apiClient
)