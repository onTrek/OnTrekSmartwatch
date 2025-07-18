package com.ontrek.mobile.screens.hike

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.ontrek.mobile.utils.components.BottomNavBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(navController: NavHostController) {

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Hikes",
                    )
                },
            )
        },
        bottomBar = { BottomNavBar(navController) }
    ) { innerPadding ->

        // TODO: Implement the content of the Hikes screen
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(
                text = "Hikes Screen"
            )
        }
    }
}