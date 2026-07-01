package com.smarthome.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.smarthome.app.ui.auth.ForgotPasswordScreen
import com.smarthome.app.ui.voice.VoiceControlScreen
import com.smarthome.app.ui.auth.LoginScreen
import com.smarthome.app.ui.auth.RegisterScreen
import com.smarthome.app.ui.chat.AiChatScreen
import com.smarthome.app.ui.device.DeviceListScreen
import com.smarthome.app.ui.home.HomeScreen
import com.smarthome.app.ui.log.LogScreen
import com.smarthome.app.ui.profile.ProfileEditScreen
import com.smarthome.app.ui.profile.ProfileScreen
import com.smarthome.app.ui.report.ReportScreen
import com.smarthome.app.ui.weather.WeatherScreen

@Composable
fun NavGraph(navController: NavHostController, startDestination: String) {
    NavHost(navController = navController, startDestination = startDestination) {
        // Auth
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(onNavigateBack = { navController.popBackStack() })
        }

        // Main tabs
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToWeather = { navController.navigate(Screen.Weather.route) },
                onNavigateToVoice = { navController.navigate(Screen.VoiceControl.route) },
                onNavigateToDevice = { navController.navigate(Screen.DeviceControl.route) },
                onNavigateToChat = { navController.navigate(Screen.Chat.route) }
            )
        }
        composable(Screen.DeviceControl.route) {
            DeviceListScreen(
                onNavigateToAiotDevices = { navController.navigate(Screen.AiotDeviceList.route) }
            )
        }
        composable(Screen.Chat.route) { AiChatScreen() }
        composable(Screen.Report.route) { ReportScreen() }
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToLogs = { navController.navigate(Screen.Logs.route) },
                onNavigateToWeather = { navController.navigate(Screen.Weather.route) },
                onNavigateToVoice = { navController.navigate(Screen.VoiceControl.route) },
                onNavigateToAiotDevices = { navController.navigate(Screen.AiotDeviceList.route) },
                onNavigateToEditProfile = { navController.navigate(Screen.ProfileEdit.route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Sub pages
        composable(Screen.Weather.route) { WeatherScreen(onNavigateBack = { navController.popBackStack() }) }
        composable(Screen.VoiceControl.route) { VoiceControlScreen(onNavigateBack = { navController.popBackStack() }) }
        composable(Screen.Logs.route) { LogScreen(onNavigateBack = { navController.popBackStack() }) }
        composable(Screen.ProfileEdit.route) { ProfileEditScreen(onNavigateBack = { navController.popBackStack() }) }
        composable(Screen.AiotDeviceList.route) { androidx.compose.material3.Text("设备管理（开发中）") }
        composable(
            route = Screen.AiotDeviceDetail.route,
            arguments = listOf(navArgument("deviceCode") { type = NavType.StringType })
        ) { androidx.compose.material3.Text("设备详情（开发中）") }
        composable(
            route = Screen.AiotDeviceForm.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType; nullable = true })
        ) { androidx.compose.material3.Text("添加设备（开发中）") }
    }
}
