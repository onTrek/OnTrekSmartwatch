// NavigationStack.kt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ontrek.wear.data.PreferencesViewModel
import com.ontrek.wear.screens.Screen
import com.ontrek.wear.screens.trackselection.TrackSelectionViewModel
import com.ontrek.wear.screens.trackselection.TrackSelectionScreen
import com.ontrek.wear.screens.sos.SOSScreen
import com.ontrek.wear.screens.track.TrackScreen

@Composable
fun NavigationStack(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    // Initialize the preferences view model to access data store
    val preferencesViewModel: PreferencesViewModel = viewModel(factory = PreferencesViewModel.Factory)

    NavHost(navController = navController, startDestination = Screen.MainScreen.route) {
        composable(route = Screen.MainScreen.route) {
            val trackSelectionViewModel = viewModel<TrackSelectionViewModel>()
            TrackSelectionScreen(
                navController = navController,
                trackListState = trackSelectionViewModel.trackListState,
                fetchTrackList = trackSelectionViewModel::fetchData,
                loadingState = trackSelectionViewModel.isLoading,
                errorState = trackSelectionViewModel.error,
                tokenState = preferencesViewModel.tokenState,
            )
        }
        composable(
            route = Screen.TrackScreen.route + "?text={text}",  // TODO: pass the filename/path of the track
            arguments = listOf(
                navArgument("text") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) {

            TrackScreen(
                navController = navController,
                text = it.arguments?.getString("text").toString(),
                modifier = modifier
            )
        }
        composable(route = Screen.SOSScreen.route) {
            SOSScreen(
                navController = navController,
            )
        }
    }
}