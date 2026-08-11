package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.RegisterScreen
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.conversations.ConversationsScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeTab
import com.example.ui.screens.profile.EditProfileScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.search.SearchScreen
import com.example.ui.screens.settings.NotificationSettingsScreen
import com.example.ui.screens.settings.PrivacyScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.splash.SplashScreen

object Destinations {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val SEARCH = "search"
    const val PROFILE = "profile/{userId}"
    const val EDIT_PROFILE = "edit_profile"
    const val CHAT = "chat/{conversationId}"
    const val SETTINGS = "settings"
    const val NOTIFICATION_SETTINGS = "notification_settings"
    const val PRIVACY = "privacy"

    fun profileRoute(userId: String) = "profile/$userId"
    fun chatRoute(conversationId: String) = "chat/$conversationId"
}

@Composable
fun WhisperNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Destinations.SPLASH,
    initialConversationId: String? = null
) {
    var selectedHomeTab by remember { mutableStateOf(HomeTab.DISCOVER) }

    NavHost(
        navController = navController,
        startDestination = if (!initialConversationId.isNullOrBlank()) Destinations.chatRoute(initialConversationId) else startDestination
    ) {
        composable(Destinations.SPLASH) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Destinations.HOME) {
                        popUpTo(Destinations.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Destinations.LOGIN) {
                        popUpTo(Destinations.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Destinations.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Destinations.HOME) {
                        popUpTo(Destinations.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Destinations.REGISTER)
                }
            )
        }

        composable(Destinations.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Destinations.HOME) {
                        popUpTo(Destinations.REGISTER) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Destinations.HOME) {
            when (selectedHomeTab) {
                HomeTab.DISCOVER -> {
                    HomeScreen(
                        onNavigateToSearch = { navController.navigate(Destinations.SEARCH) },
                        onNavigateToProfile = { userId -> navController.navigate(Destinations.profileRoute(userId)) },
                        onNavigateToChat = { convId -> navController.navigate(Destinations.chatRoute(convId)) },
                        onNavigateToSettings = { navController.navigate(Destinations.SETTINGS) },
                        selectedTab = selectedHomeTab,
                        onTabSelected = { selectedHomeTab = it }
                    )
                }
                HomeTab.CONVERSATIONS -> {
                    ConversationsScreen(
                        onNavigateToChat = { convId -> navController.navigate(Destinations.chatRoute(convId)) },
                        onNavigateToSearch = { navController.navigate(Destinations.SEARCH) },
                        onNavigateToProfile = { userId -> navController.navigate(Destinations.profileRoute(userId)) },
                        onNavigateBack = { selectedHomeTab = HomeTab.DISCOVER }
                    )
                }
                HomeTab.SETTINGS -> {
                    SettingsScreen(
                        onNavigateBack = { selectedHomeTab = HomeTab.DISCOVER },
                        onNavigateToEditProfile = { navController.navigate(Destinations.EDIT_PROFILE) },
                        onNavigateToNotificationSettings = { navController.navigate(Destinations.NOTIFICATION_SETTINGS) },
                        onNavigateToPrivacy = { navController.navigate(Destinations.PRIVACY) },
                        onLogout = {
                            navController.navigate(Destinations.LOGIN) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }

        composable(Destinations.SEARCH) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { userId -> navController.navigate(Destinations.profileRoute(userId)) },
                onNavigateToChat = { convId -> navController.navigate(Destinations.chatRoute(convId)) }
            )
        }

        composable(
            route = Destinations.PROFILE,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ProfileScreen(
                userId = userId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { convId -> navController.navigate(Destinations.chatRoute(convId)) },
                onNavigateToEditProfile = { navController.navigate(Destinations.EDIT_PROFILE) }
            )
        }

        composable(Destinations.EDIT_PROFILE) {
            EditProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Destinations.CHAT,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            ChatScreen(
                conversationId = conversationId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { userId -> navController.navigate(Destinations.profileRoute(userId)) }
            )
        }

        composable(Destinations.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditProfile = { navController.navigate(Destinations.EDIT_PROFILE) },
                onNavigateToNotificationSettings = { navController.navigate(Destinations.NOTIFICATION_SETTINGS) },
                onNavigateToPrivacy = { navController.navigate(Destinations.PRIVACY) },
                onLogout = {
                    navController.navigate(Destinations.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Destinations.NOTIFICATION_SETTINGS) {
            NotificationSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Destinations.PRIVACY) {
            PrivacyScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
