package com.nebula.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nebula.app.core.common.LocalAppContainer
import com.nebula.app.core.common.LocalDemoMode
import com.nebula.app.core.common.LocalSnackbarHost
import com.nebula.app.core.navigation.NebulaNavHost
import com.nebula.app.core.navigation.Routes
import com.nebula.app.core.navigation.topLevelDestinations
import com.nebula.app.design.components.NebulaBottomBar
import com.nebula.app.design.theme.NebulaColors
import com.nebula.app.design.theme.NebulaTheme

class MainActivity : ComponentActivity() {
    private var navController: NavHostController? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        navController?.handleDeepLink(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        val container = (application as NebulaApp).container
        setContent {
            NebulaTheme {
                CompositionLocalProvider(
                    LocalAppContainer provides container,
                    LocalDemoMode provides container.isDemoMode
                ) {
                    NebulaRoot(onNavController = { navController = it })
                }
            }
        }
    }
}

@Composable
private fun NebulaRoot(onNavController: (NavHostController) -> Unit) {
    val navController = rememberNavController()
    androidx.compose.runtime.SideEffect { onNavController(navController) }
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = topLevelDestinations.any { it.route == currentRoute }

    CompositionLocalProvider(LocalSnackbarHost provides snackbarHostState) {
        Scaffold(
            containerColor = NebulaColors.Background,
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = NebulaColors.Ink,
                        contentColor = androidx.compose.ui.graphics.Color.White
                    )
                }
            },
            bottomBar = {
                if (showBottomBar) {
                    NebulaBottomBar(
                        destinations = topLevelDestinations,
                        currentRoute = currentRoute,
                        onSelect = { route ->
                            navController.navigate(route) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            // Tab screens apply innerPadding themselves; other screens sit inside the safe area.
            Box(modifier = Modifier.padding(if (showBottomBar) androidx.compose.foundation.layout.PaddingValues() else innerPadding)) {
                NebulaNavHost(navController = navController, innerPadding = innerPadding)
            }
        }
    }
}
