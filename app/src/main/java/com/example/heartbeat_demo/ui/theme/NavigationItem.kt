package com.example.heartbeat_demo.viewmodel

import androidx.annotation.DrawableRes
import com.example.heartbeat_demo.R

// Bottom navigation item data class
data class NavigationItem(
    val name: String,
    @DrawableRes val icon: Int,
    val title: String
) {
    companion object {
        val DATA_MANAGEMENT = NavigationItem("data_management", R.drawable.shujuku, "数据管理")
        val DEVICE_CONNECTION = NavigationItem("device_connection", R.drawable.bluetooth_fill, "设备连接")
        val USER_MANAGEMENT = NavigationItem("user_management", R.drawable.guanyu_us, "用户设置")
    }
}