package com.smarthome.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val screen: Screen
)

val bottomNavItems = listOf(
    BottomNavItem("首页", Icons.Default.Home, Screen.Home),
    BottomNavItem("设备", Icons.Default.Power, Screen.DeviceControl),
    BottomNavItem("报表", Icons.Default.BarChart, Screen.Report),
    BottomNavItem("AI", Icons.Default.Chat, Screen.Chat),
    BottomNavItem("我的", Icons.Default.Person, Screen.Profile)
)
