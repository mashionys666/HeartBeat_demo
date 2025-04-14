package com.example.heartbeat_demo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class NavigationViewModel : ViewModel() {
    sealed class NavigationEvent {
        data object NavigateToHome : NavigationEvent()
        data object NavigateToDeviceConnection : NavigationEvent()
        data object NavigateToUserManagement : NavigationEvent()
    }

    // Use a SharedFlow to emit navigation events
    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents.asSharedFlow()

    // Manage the checkboxes in the bottom navigation bar, which defaults to “Data Management”.
    private val _selectedNavItem = MutableStateFlow(NavigationItem.DATA_MANAGEMENT)
    val selectedNavItem: StateFlow<NavigationItem> = _selectedNavItem

    // Update the checkmarks in the bottom navigation bar
    fun selectNavItem(item: NavigationItem) {
        _selectedNavItem.value = item
    }
    fun emitNavigationEvent(event: NavigationEvent) {
        viewModelScope.launch {
            _navigationEvents.emit(event)
        }
    }
}