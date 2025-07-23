package com.ontrek.wear.screens

sealed class Screen(val route: String) {
    object MainScreen : Screen("HomeScreen")
    object TrackSelectionScreen : Screen("TrackSelectionScreen")
    object HikeSelectionScreen : Screen("HikeSelectionScreen")
    object TrackScreen : Screen("TrackScreen")
    object SOSScreen : Screen("SOSScreen")
}