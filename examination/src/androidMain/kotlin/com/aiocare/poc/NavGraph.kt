package com.aiocare.poc

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aiocare.Screens
import com.aiocare.cat.Cat
import com.aiocare.custom.CustomScreen
import com.aiocare.flow.FlowScreen
import com.aiocare.mvvm.createDefaultConfig
import com.aiocare.poc.calibration.CalibrationScreen
import com.aiocare.poc.calibration.CalibrationViewModel
import com.aiocare.poc.flow.FlowViewModel
import com.aiocare.poc.input.HansInputScreen
import com.aiocare.poc.input.HansInputViewModel
import com.aiocare.poc.intro.IntroScreen
import com.aiocare.poc.intro.IntroViewModel
import com.aiocare.poc.searchDevice.SearchDeviceScreen
import com.aiocare.poc.searchDevice.SearchDeviceViewModel
import com.aiocare.poc.superCat.SuperCatScreen
import com.aiocare.poc.superCat.SuperCatViewModel
import com.aiocare.poc.superCat.custom.CustomViewModel

@Composable
fun NavGraph (navController: NavHostController){
    NavHost(
        navController = navController,
        startDestination = Screens.Intro.route
    )
    {
        composable(route = Screens.Cat.route) {
            Cat()
        }
        composable(route = Screens.Intro.route) {
            IntroScreen(
                viewModel = IntroViewModel(createDefaultConfig()),
                navController = navController
            )
        }
        composable(route = Screens.Custom.route) {
            CustomScreen(
                viewModel = customCatVM,
                navController = navController
            )
        }
        composable(route = Screens.Flow.route) {
            FlowScreen(
                viewModel = flowCatVM,
                navController = navController
            )
        }
        composable(route = Screens.SearchDevice.route) {
            SearchDeviceScreen(viewModel = SearchDeviceViewModel(createDefaultConfig()))
        }
        composable(route = Screens.HansInput.route) {
            HansInputScreen(
                viewModel = HansInputViewModel(createDefaultConfig()),
                navController = navController
            )
        }
        composable(route = Screens.Calibration.route) {
            CalibrationScreen(
                viewModel = CalibrationViewModel(createDefaultConfig())
            )
        }
        composable(route = Screens.SuperCat.route) {
            SuperCatScreen(
                viewModel = superCatVM,
                navController = navController
            )
        }
    }
}

val superCatVM by lazy { SuperCatViewModel(createDefaultConfig()) }
val customCatVM by lazy { CustomViewModel(createDefaultConfig()) }
val flowCatVM by lazy { FlowViewModel(createDefaultConfig()) }