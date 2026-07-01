package com.smarthome.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smarthome.app.core.datastore.AppSettingsDataStore
import com.smarthome.app.core.datastore.TokenDataStore
import com.smarthome.app.core.network.WebSocketManager
import com.smarthome.app.ui.navigation.NavGraph
import com.smarthome.app.ui.navigation.Screen
import com.smarthome.app.ui.navigation.bottomNavItems
import com.smarthome.app.ui.theme.DoubaoOrange
import com.smarthome.app.ui.theme.SmartHomeTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.get

class MainActivity : ComponentActivity() {

    private val tokenDataStore: TokenDataStore by lazy { get(TokenDataStore::class.java) }
    private val webSocketManager: WebSocketManager by lazy { get(WebSocketManager::class.java) }
    private val appSettingsDataStore: AppSettingsDataStore by lazy { get(AppSettingsDataStore::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SmartHomeTheme {
                SmartHomeMainScreen(
                    tokenDataStore = tokenDataStore,
                    webSocketManager = webSocketManager
                )
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val isLoggedIn = tokenDataStore.isLoggedIn.first()
                if (isLoggedIn) {
                    val host = appSettingsDataStore.getBaseUrl()
                    webSocketManager.connect(host)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocketManager.disconnect()
    }
}

@Composable
fun SmartHomeMainScreen(
    tokenDataStore: TokenDataStore,
    webSocketManager: WebSocketManager
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavScreens = bottomNavItems.map { it.screen.route }
    val showBottomBar = currentDestination?.route in bottomNavScreens

    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val isLoggedIn = tokenDataStore.isLoggedIn.first()
        startDestination = if (isLoggedIn) Screen.Home.route else Screen.Login.route
    }

    startDestination?.let { start ->
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        item.icon,
                                        contentDescription = item.label,
                                        tint = if (selected) DoubaoOrange else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                label = {
                                    Text(
                                        item.label,
                                        color = if (selected) DoubaoOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = DoubaoOrange.copy(alpha = 0.12f)
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Surface(modifier = Modifier.fillMaxSize().padding(innerPadding), color = MaterialTheme.colorScheme.background) {
                NavGraph(navController = navController, startDestination = start)
            }
        }
    }
}
