package com.beradeep.aiyo.nav

sealed interface Screen {
    data object ChatScreen : Screen
    data object SettingsScreen : Screen
}