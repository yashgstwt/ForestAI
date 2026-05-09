package com.theo.forest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.theo.forest.data.modal.Response
import com.theo.forest.ui.screens.AuthScreen
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.theo.forest.ui.viewmodals.HomeViewModal
import com.theo.forest.ui.screens.DetailScreen
import com.theo.forest.ui.screens.HomeScreen
import com.theo.forest.ui.theme.ForestTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Serializable
    sealed class Screens {
        @Serializable
        data object AUTH
        @Serializable
        data object HOME
        @Serializable
        data object DETAIL
        @Serializable
        data object WEATHER
        @Serializable
        data object HISTORY
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            ForestTheme(dynamicColor = false) {
                val viewModel: HomeViewModal = hiltViewModel()
                val authState by viewModel.authState.collectAsState()

                when (authState) {
                    is Response.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    else -> {
                        NavHost(
                            navController = navController,
                            startDestination = if (authState is Response.Success) Screens.HOME else Screens.AUTH,
                        ) {
                            composable<Screens.AUTH> {
                                AuthScreen(
                                    viewModal = viewModel,
                                    onAuthSuccess = {
                                        navController.navigate(Screens.HOME) {
                                            popUpTo(Screens.AUTH) { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable<Screens.HOME> {
                                HomeScreen(
                                    viewModal = viewModel,
                                    navToDetail = {
                                        navController.navigate(Screens.DETAIL)
                                    },
                                    navToWeather = {
                                        navController.navigate(Screens.WEATHER)
                                    },
                                    navToHistory = {
                                        navController.navigate(Screens.HISTORY)
                                    },
                                    onLogout = {
                                        navController.navigate(Screens.AUTH) {
                                            popUpTo(Screens.HOME) { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable<Screens.HISTORY> {
                                com.theo.forest.ui.screens.HistoryScreen(
                                    viewModal = viewModel,
                                    navToDetail = {
                                        navController.navigate(Screens.DETAIL)
                                    },
                                    navToWeather = {
                                        navController.navigate(Screens.WEATHER)
                                    },
                                    navToHome = {
                                        navController.navigate(Screens.HOME)
                                    }
                                )
                            }
                            composable<Screens.DETAIL> {
                                DetailScreen(
                                    viewModal = viewModel,
                                    backPress = { navController.popBackStack() }
                                )
                            }
                            composable<Screens.WEATHER> {
                                com.theo.forest.ui.screens.WeatherForecastScreen(
                                    viewModal = viewModel,
                                    navToHome = {
                                        navController.navigate(Screens.HOME) {
                                            popUpTo(Screens.HOME) { inclusive = true }
                                        }
                                    },
                                    navToHistory = {
                                        navController.navigate(Screens.HISTORY)
                                    },
                                    backPress = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
