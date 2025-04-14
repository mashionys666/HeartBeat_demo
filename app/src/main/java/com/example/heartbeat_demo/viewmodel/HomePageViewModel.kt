package com.example.heartbeat_demo.viewmodel

import androidx.lifecycle.ViewModel

class HomePageViewModel( ) : ViewModel() {

    // 处理卡片点击，导航到对应监控模式
    fun navigateToMonitoringMode(mode: MonitoringMode) {
        // TODO: 使用NavController实现具体导航
//        MonitoringMode.SMART_MONITORING -> navController.navigate("smart_monitoring")
//        MonitoringMode.MULTI_MODE_HEART_LUNG -> navController.navigate("multi_mode_heart_lung")
//        MonitoringMode.PULSE_OXIMETRY -> navController.navigate("pulse_oximetry")
//        MonitoringMode.PULMONARY_ARTERY_OXIMETRY -> navController.navigate("pulmonary_artery_oximetry")
    }
}

// 监控模式枚举类
enum class MonitoringMode(val displayName: String) {
    SMART_MONITORING("智能监控"),           // 智能监控
    MULTI_MODE_HEART_LUNG("多模态心肺监护"),     // 多模态心肺监护
    PULSE_OXIMETRY("颈动脉血流监护"),            // 颈动脉血流监护
    PULMONARY_ARTERY_OXIMETRY("肺动脉血流监护")  // 肺动脉血流监护
}

