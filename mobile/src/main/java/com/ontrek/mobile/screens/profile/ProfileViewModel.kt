package com.ontrek.mobile.screens.profile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.Context
import android.util.Log
import com.ontrek.shared.api.profile.deleteProfile
import com.ontrek.shared.api.profile.getProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update

data class UserProfile(
    val username: String = "Update...",
    val email: String = "Update...",
    val userId: String = "Update...",
)

class ProfileViewModel : ViewModel() {

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isLoadingConnection = MutableStateFlow(false)
    val isLoadingConnection = _isLoadingConnection.asStateFlow()

    private val _isLoadingDeleteProfile = MutableStateFlow(false)
    val isLoadingDeleteProfile = _isLoadingDeleteProfile.asStateFlow()

    private val _connectionStatus = MutableStateFlow(false)
    val connectionStatus: StateFlow<Boolean> = _connectionStatus.asStateFlow()

    private val _msgToast = MutableStateFlow("")
    val msgToastFlow: StateFlow<String> = _msgToast.asStateFlow()

    fun fetchUserProfile(token: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                getProfile(
                    token = token,
                    onSuccess = { response ->
                        _userProfile.update {
                            UserProfile(
                                username = response?.username ?: "",
                                email = response?.email ?: "",
                                userId = response?.id ?: "",
                            )
                        }
                    },
                    onError = { error ->
                        Log.e("ProfileViewModel", "Error fetching profile: $error")
                        _userProfile.update {
                            UserProfile(
                                username = "Error",
                                email = "Error",
                                userId = "Error"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("ProfileView", "Error fetching user profile", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchDeleteProfile(navigateToLogin: () -> Unit, token: String) {
        viewModelScope.launch {
            _isLoadingDeleteProfile.value = true

            try {
                 deleteProfile(
                     token = token,
                     onSuccess = {
                         _userProfile.update { UserProfile(
                             username = "",
                             email = "",
                             userId = ""
                         ) } // Reset profile after deletion
                         _msgToast.value = "Profile deleted successfully"
                         navigateToLogin()
                     },
                     onError = { error ->
                         Log.e("ProfileViewModel", "Error deleting profile: $error")
                         _msgToast.value = "Impossible to delete profile: $error"
                     }
                 )
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error deleting profile", e)
            } finally {
                delay(500)
                _isLoadingDeleteProfile.value = false
            }
        }
    }

    fun sendAuthToWearable(context: Context, token: String) {
        viewModelScope.launch {
            _isLoadingConnection.value = true
            try {
                val putDataMapReq = PutDataMapRequest.create("/auth").apply {
                    dataMap.putString("token", token)
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }
                val request = putDataMapReq.asPutDataRequest().setUrgent()

                Wearable.getDataClient(context).putDataItem(request)
                    .addOnSuccessListener {
                        Log.d("WATCH_CONNECTION", "Connessione riuscita con il wearable")
                        _connectionStatus.value = true
                        _msgToast.value = "Connected to wearable successfully"
                    }
                    .addOnFailureListener {
                        Log.e("WATCH_CONNECTION", "Fallita connessione con il wearable", it)
                        _connectionStatus.value = false
                        _msgToast.value = "Failed to connect to wearable"
                    }
            } catch (e: Exception) {
                Log.e("WATCH_CONNECTION", "Errore durante la connessione al wearable", e)
                _connectionStatus.value = false
                _msgToast.value = "Error connecting to wearable: ${e.message}"
            } finally {
                delay(500)
                _isLoadingConnection.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _msgToast.value = "This feature is not implemented yet"
        }
    }
    fun resetMsgToast() {
        viewModelScope.launch {
            _msgToast.value = ""
        }
    }
}