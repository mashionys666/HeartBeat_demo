package com.example.heartbeat_demo

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.heartbeat_demo.viewmodel.DeviceConnectionViewModel

class DeviceConnectionViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeviceConnectionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DeviceConnectionViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}