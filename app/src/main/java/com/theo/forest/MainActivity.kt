package com.theo.forest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
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
        data object HOME
        @Serializable
        data object DETAIL
        @Serializable
        data object WEATHER
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            ForestTheme(dynamicColor = false) {
                val viewModel: HomeViewModal = hiltViewModel()
                NavHost(
                    navController = navController,
                    startDestination = Screens.HOME,
                ) {
                    composable<Screens.HOME> {
                        HomeScreen(
                            viewModal = viewModel,
                            navToDetail = {
                                navController.navigate(Screens.DETAIL)
                            },
                            navToWeather = {
                                navController.navigate(Screens.WEATHER)
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
                            backPress = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
