package com.example.heartbeat_demo.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.heartbeat_demo.AudioData.AudioPlaybackManager


interface DataInterface {
    val isBluetoothConnected: LiveData<Boolean>
}

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



