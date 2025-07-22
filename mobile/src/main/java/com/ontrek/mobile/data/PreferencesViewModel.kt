package com.ontrek.mobile.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ontrek.mobile.StoreApplication
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PreferencesViewModel(
    private val preferencesStore: PreferencesStore
): ViewModel() {

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as StoreApplication)
                PreferencesViewModel(application.preferencesStore)
            }
        }
    }

    val tokenState: StateFlow<String?> =
        preferencesStore.currentToken.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val currentUserState: StateFlow<String?> =
        preferencesStore.currentUser.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    fun saveToken(token: String) {
        viewModelScope.launch {
            preferencesStore.saveToken(token)
        }
    }

    fun saveCurrentUser(userId: String) {
        viewModelScope.launch {
            preferencesStore.saveCurrentUser(userId)
        }
    }
}