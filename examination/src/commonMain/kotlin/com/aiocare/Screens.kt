package com.aiocare

sealed class Screens(val route: String) {
    data object Intro : Screens("Intro")
    data object SearchDevice : Screens("SearchDevice")
    data object Cat : Screens("Cat")
    data object HansInput : Screens("HansInput")
    data object Calibration : Screens("Calibration")
    data object SuperCat : Screens("SuperCat")
}
