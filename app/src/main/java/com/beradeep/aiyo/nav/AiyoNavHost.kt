package com.beradeep.aiyo.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavKey
import androidx.navigation3.ui.NavHost
import androidx.navigation3.ui.rememberNavController
import com.beradeep.aiyo.ui.screens.chat.ChatScreen
import com.beradeep.aiyo.ui.screens.settings.SettingsScreen
import com.beradeep.aiyo.ui.screens.chat.ChatViewModel
import com.beradeep.aiyo.ui.screens.settings.SettingsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel

sealed interface Screen : NavKey {
    data object ChatScreen : Screen
    data object SettingsScreen : Screen
}

@Composable
fun AiyoNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNav Controller()
    val chatViewModel: ChatViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.ChatScreen,
        modifier = modifier
    ) {
        scene<Screen.ChatScreen> {
            ChatScreen(
                viewModel = chatViewModel,
                onNavigateToSettings = { navController.navigate(Screen.SettingsScreen) }
            )
        }
        scene<Screen.SettingsScreen> {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}