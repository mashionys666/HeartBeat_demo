package com.example.heartbeat_demo.Interface

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.heartbeat_demo.R
import com.example.heartbeat_demo.viewmodel.HomePageViewModel
import com.example.heartbeat_demo.viewmodel.MonitoringMode
import com.example.heartbeat_demo.viewmodel.NavigationViewModel

@Composable
fun HomePageUI(
    navController: NavHostController,
    navigationViewModel: NavigationViewModel,
    homeViewModel: HomePageViewModel
) {
    AppScaffold(
        navController, navigationViewModel,
        drawerContent = { MyNavigationDrawer() }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF0F8FF)), // 极浅蓝色背景
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 副标题
            Text(
                text = "选择监控模式",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
            // 2x2网络布局
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                items(4) { index ->
                    val mode = when (index) {
                        0 -> MonitoringMode.SMART_MONITORING
                        1 -> MonitoringMode.MULTI_MODE_HEART_LUNG
                        2 -> MonitoringMode.PULSE_OXIMETRY
                        3 -> MonitoringMode.PULMONARY_ARTERY_OXIMETRY
                        else -> MonitoringMode.SMART_MONITORING
                    }
                    val title = when (mode) {
                        MonitoringMode.SMART_MONITORING -> "气泡监测"
                        MonitoringMode.MULTI_MODE_HEART_LUNG -> "心肺监护"
                        MonitoringMode.PULSE_OXIMETRY -> "颈动脉血流监护"
                        MonitoringMode.PULMONARY_ARTERY_OXIMETRY -> "肺动脉血流监护"
                    }
                    val icon: Painter = when (mode) {
                        MonitoringMode.SMART_MONITORING -> painterResource(id = R.drawable.qipao)
                        MonitoringMode.MULTI_MODE_HEART_LUNG -> painterResource(id = R.drawable.xinzang)
                        MonitoringMode.PULSE_OXIMETRY -> painterResource(id = R.drawable.jingdongmai)
                        MonitoringMode.PULMONARY_ARTERY_OXIMETRY -> painterResource(id = R.drawable.fei)
                        else -> painterResource(id = R.drawable.ic_launcher_background) // Default icon
                    }
                    val backgroundColor = if (index == 3) Color(0xFF87CEEB) else Color(0xFFADD8E6) // 右下深蓝色

                    Card(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(160.dp) // Set a fixed size for the card
                            .aspectRatio(1f) // Maintain a 1:1 aspect ratio
                            .clickable {
                                navController.navigate("data_interface/${mode.name}")
                            },
                        shape = RoundedCornerShape(8.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = backgroundColor
                        ),
                        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Icon(
                                painter = icon,
                                contentDescription = title,
                                tint = Color.Black,
                                modifier = Modifier.size(48.dp) // Set a smaller size for the icon
                            )
                            Spacer(modifier = Modifier.height(4.dp))// Reduce the space between icon and text
                            Text(
                                text = title,
                                color = Color.Black,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}