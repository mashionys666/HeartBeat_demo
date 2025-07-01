package com.example.heartbeat_demo.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.heartbeat_demo.AudioData.AudioPlaybackManager


/**
 * DataInterface for managing Bluetooth connection status
 * 
 * Design Decision: This interface provides ONLY basic Bluetooth connection status.
 * Automatic heartbeat functionality has been intentionally excluded to:
 * - Reduce unnecessary network traffic
 * - Prevent communication overhead 
 * - Simplify the connection management
 * 
 * Connection status is determined by the underlying Bluetooth GATT connection state.
 * DO NOT add heartbeat packet sending, communication health checking, or periodic
 * status validation to this interface.
 */
interface DataInterface {
    val isBluetoothConnected: LiveData<Boolean>
}

/**
 * DataInterfaceViewModel manages the data interface functionality for the application.
 * 
 * IMPORTANT: This ViewModel intentionally does NOT include heartbeat functionality.
 * The connection status is based solely on the Bluetooth GATT connection state.
 * 
 * Heartbeat functionality has been explicitly excluded to avoid:
 * - Automatic packet sending every 5 seconds
 * - Communication health status tracking
 * - Response validation and timeout handling
 * - Unnecessary battery drain and network overhead
 * 
 * Any future modifications should maintain this design decision.
 */
class dataInterfaceViewModel(
    private val deviceConnectionViewModel: DeviceConnectionViewModel,
    private val modeName: String
) : ViewModel() , DataInterface {

    private val audioPlaybackManager = AudioPlaybackManager()
    val pcmData :LiveData<ByteArray> = audioPlaybackManager.pcmData
    // 设置蓝牙数据回调
    fun setAudioDataCallback(deviceViewModel: DeviceConnectionViewModel) {
        deviceViewModel.dataCallback = audioPlaybackManager
    }
    // 音频控制功能
    fun startPlayback() {
        audioPlaybackManager.startPlayback()
    }

    fun pausePlayback() {
        audioPlaybackManager.pausePlayback()
    }

    fun stopPlayback() {
        audioPlaybackManager.stopPlayback()
    }

    override fun onCleared() {
        super.onCleared()
        audioPlaybackManager.release()
    }
    /**
     * Bluetooth connection status derived from GATT connection state.
     * This provides real-time connection status without requiring heartbeat packets.
     * The MediatorLiveData automatically updates when the underlying GATT connection changes.
     */
    override val isBluetoothConnected: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(deviceConnectionViewModel.isConnected) { isConnected ->
            value = isConnected
        }
    }
}

class DataInterfaceViewModelFactory(
    private val context: Context,
    private val deviceConnectionViewModel: DeviceConnectionViewModel,
    private val modeName: String // New parameter
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(dataInterfaceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return dataInterfaceViewModel(deviceConnectionViewModel, modeName) as T // Pass modeName here
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}



