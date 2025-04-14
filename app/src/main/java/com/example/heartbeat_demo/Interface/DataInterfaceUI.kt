package com.example.heartbeat_demo.Interface

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.heartbeat_demo.AudioData.RealTimeWaveformView
import com.example.heartbeat_demo.AudioData.WaveformViewModel
import com.example.heartbeat_demo.R
import com.example.heartbeat_demo.viewmodel.DeviceConnectionViewModel
import com.example.heartbeat_demo.viewmodel.MonitoringMode
import com.example.heartbeat_demo.viewmodel.NavigationViewModel
import com.example.heartbeat_demo.viewmodel.dataInterfaceViewModel
import java.lang.Math.pow
import kotlin.math.abs
import kotlin.math.min

@RequiresApi(Build.VERSION_CODES.R)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataInterfaceUI(
    navController: NavHostController,
    navigationViewModel: NavigationViewModel,
    dataInterfaceViewModel: dataInterfaceViewModel,
    deviceConnectionViewModel: DeviceConnectionViewModel
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val selectedModeName = navBackStackEntry?.arguments?.getString("modeName")
    val selectedMode = MonitoringMode.values().find { it.name == selectedModeName }
    val title = selectedMode?.displayName ?: "Data Interface"

    var isPlaying by remember { mutableStateOf(false) }
    val pcmData by dataInterfaceViewModel.pcmData.observeAsState(initial = ByteArray(0))
    var  showFloatingScreen by remember { mutableStateOf(false ) }
    val  waveformViewModel : WaveformViewModel = viewModel()

    LaunchedEffect(Unit) {
        dataInterfaceViewModel.setAudioDataCallback(deviceConnectionViewModel)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    // 可选：恢复时开始数据轮询
                }
                Lifecycle.Event.ON_PAUSE -> {
                    // 暂停播放但不停止数据轮询
                    dataInterfaceViewModel.pausePlayback()
                    isPlaying = false
                }
                Lifecycle.Event.ON_DESTROY -> {
                    // ViewModel 会自动处理释放资源，这里不需要显式调用
                    // dataInterfaceViewModel 的 onCleared 方法会调用 audioPlaybackManager.release()
                }
                else -> {} // 处理其他生命周期事件（如需要）
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AppScaffold(navController, navigationViewModel, drawerContent = { MyNavigationDrawer() })
    {
        innerPadding ->
        Column (
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF0F8FF))
        ){
            //Top Bar
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            textAlign = TextAlign.Center,
                            color = Color.Black
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Color(0xFFADD8E6)
                )
            )

            // Connection Status
            ConnectionStatusSection(dataInterfaceViewModel)

            // Data Display Area
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                state = rememberLazyListState()
            ){
                item {
                    // Data Boxes Container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clip(RoundedCornerShape(10.dp)) // Rounded corners
                            .border(2.dp, Color.Black, RoundedCornerShape(10.dp))
                            // Bold black border with rounded corners
                            .background(Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Data Box 1 (Audio Playback)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .padding(8.dp)
                                    .background(Color.White)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Button(onClick = {
                                            if (isPlaying) {
                                                dataInterfaceViewModel.pausePlayback()
                                            } else {
                                                dataInterfaceViewModel.startPlayback()
                                            }
                                            isPlaying = !isPlaying
                                        }) {
                                            Text(if (isPlaying) "Pause" else "Play")
                                        }
                                        Button(onClick = {
                                            dataInterfaceViewModel.stopPlayback()
                                            isPlaying = false
                                        }) {
                                            Text("Stop")
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    AudioVisualizer(pcmData)
                                }
                            }
                            // Data Box 2
                                Button(
                                    onClick = { showFloatingScreen = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .padding(8.dp)
                                        .background(Color.White),
                                    shape = RectangleShape,
                                    border = BorderStroke(1.dp, Color.Black),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                                ) {
                                    Text(
                                        text = "心音波形图",
                                        modifier = Modifier.align(Alignment.CenterVertically),
                                        color = Color.Black
                                    )
                                }
                            // 显示横屏波形图对话框
                            if (showFloatingScreen) {
                                HorizontalScreenDialog(
                                    onDismiss = { showFloatingScreen = false },
                                    content = {
                                        HorizontalWaveformContent(waveformViewModel)
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    // Control Buttons Container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clip(RoundedCornerShape(10.dp)) // Rounded corners
                            .border(2.dp, Color.Black, RoundedCornerShape(10.dp))
                            // Bold black border with rounded corners
                            .background(Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ControlButton(
                                    text = "开始",
                                    iconId = R.drawable.fasong,
                                    buttonSize = 80.dp
                                ) {
                                    // Handle Start button click
                                }
                                ControlButton(
                                    text = "暂停",
                                    iconId = R.drawable.baocunxiazai,
                                    buttonSize = 80.dp
                                ) {
                                    // Handle Pause button click
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ControlButton(
                                    text = "保存",
                                    iconId = R.drawable.baocunxiazai,
                                    buttonSize = 80.dp
                                ) {
                                }
                                ControlButton(
                                    text = "打开",
                                    iconId = R.drawable.data_icon,
                                    buttonSize = 80.dp
                                ) {
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AudioVisualizer(pcmData: ByteArray) {
    val barCount = 30  // 可视化器条形数量
    val barHeights = remember { mutableStateListOf<Float>() }
    val previousBarHeights = remember { mutableStateListOf<Float>() }

    // 初始化高度数组
    LaunchedEffect(Unit) {
        if (barHeights.isEmpty()) {
            repeat(barCount) { barHeights.add(0f) }
        }
        if (previousBarHeights.isEmpty()) {
            repeat(barCount) { previousBarHeights.add(0f) }
        }
    }

    // 响应PCM数据变化
    LaunchedEffect(pcmData) {
        if (pcmData.isNotEmpty()) {
            val newBarHeights = analyzeAudioData(pcmData, barCount, previousBarHeights)
            for (i in 0 until minOf(barCount, newBarHeights.size)) {
                barHeights[i] = newBarHeights[i]
                previousBarHeights[i] = newBarHeights[i]
            }
        }
    }

    // 添加渐变动画，使静止时也有轻微波动
    val animatedBarHeights = barHeights.map { height ->
        val animatedHeight by animateFloatAsState(
            targetValue = height,
            animationSpec = tween(durationMillis = 150)
        )
        animatedHeight
    }

    // 绘制可视化效果
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(Color(0xFFF8F8F8), RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        val barWidth = size.width / barCount
        val maxHeight = size.height

        for (i in 0 until barCount) {
            val barHeight = if (i < animatedBarHeights.size) {
                animatedBarHeights[i] * maxHeight
            } else {
                0f
            }

            // 使用渐变色，低振幅蓝色，高振幅红色
            val barColor = lerp(
                Color(0xFF2196F3), // 蓝色
                Color(0xFFE91E63), // 粉红色
                if (i < animatedBarHeights.size) animatedBarHeights[i] else 0f
            )

            drawLine(
                color = barColor,
                start = Offset(i * barWidth + barWidth / 2, maxHeight),
                end = Offset(i * barWidth + barWidth / 2, maxHeight - barHeight),
                strokeWidth = barWidth * 0.8f,
                cap = StrokeCap.Round
            )
        }
    }
}
fun analyzeAudioData(pcmData: ByteArray, barCount: Int, previousBarHeights: List<Float>): List<Float> {
    if (pcmData.isEmpty()) {
        return List(barCount) { 0f }
    }

    val barHeights = mutableListOf<Float>()
    val samplesPerBar = (pcmData.size / 2) / barCount

    for (i in 0 until barCount) {
        var sum = 0.0
        var count = 0

        val startSample = i * samplesPerBar
        val endSample = minOf((i + 1) * samplesPerBar, pcmData.size / 2)

        for (sampleIndex in startSample until endSample) {
            val byteIndex = sampleIndex * 2
            if (byteIndex + 1 < pcmData.size) {
                // 小端序16位PCM解析
                val sample = (pcmData[byteIndex].toInt() and 0xFF) or
                        ((pcmData[byteIndex+1].toInt() and 0xFF) shl 8)
                // 转换为有符号值
                val signedSample = if (sample > 32767) sample - 65536 else sample
                sum += abs(signedSample) / 32768.0
                count++
            }
        }

        val avgAmplitude = if (count > 0) sum / count else 0.0

        // 应用非线性缩放，增强低振幅可视性
        val enhancedHeight = min(1.0, pow(avgAmplitude * 1.5, 0.7))

        // 平滑处理
        val smoothedHeight = if (previousBarHeights.size > i) {
            enhancedHeight * 0.3 + previousBarHeights[i] * 0.7
        } else {
            enhancedHeight
        }

        barHeights.add(smoothedHeight.toFloat())
    }

    return barHeights
}

@Composable
fun ConnectionStatusSection(dataInterfaceViewModel: dataInterfaceViewModel) {

    val isConnected by dataInterfaceViewModel.isBluetoothConnected.observeAsState(initial = false)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "连接状态：", color = Color.Black)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isConnected) "Connected" else "Not Connected",
            color = if (isConnected) Color.Green else Color.Red
        )
    }
}

@Composable
fun ControlButton(text: String, iconId: Int, buttonSize: Dp, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(buttonSize), // Use the passed buttonSize
        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = iconId),
                contentDescription = text,
                modifier = Modifier.size(buttonSize / 2)
            )
            Text(
                text = text,
                fontSize = 12.sp,
                color = Color.Black
            )
        }
    }
}

@Composable
fun HorizontalScreenDialog(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)  // 占据90%的宽度
                    .fillMaxHeight(1.0f)  // 占据100%的高度
                    .align(Alignment.Center)
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .shadow(8.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // 顶部控制栏
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "关闭",
                                tint = Color.Black
                            )
                        }

                        Text(
                            text = "横屏图像预览",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Black
                        )

                        // 右侧可以放置其他控制按钮
                        IconButton(onClick = { /* 其他功能，如全屏 */ }) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "全屏",
                                tint = Color.Black
                            )
                        }
                    }

                    // 内容区域 - 使用BoxWithConstraints来处理横屏布局
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF5F5F5))  // 轻微灰色背景
                            .padding(8.dp)
                    ) {
                        val screenWidth = maxWidth
                        val screenHeight = maxHeight

                        // 创建一个横向容器，使内容以横向模式展示
                        Box(
                            modifier = Modifier
                                .width(screenHeight)  // 交换宽高
                                .height(screenWidth)
                                .align(Alignment.Center)
                                .graphicsLayer {
                                    rotationZ = 90f  // 使用图形旋转而不是简单的rotate
                                    // 使用toPx()将Dp转换为像素(Float)
                                    translationX = (screenHeight.toPx() - screenWidth.toPx()) / 2
                                    translationY = (screenWidth.toPx() - screenHeight.toPx()) / 2
                                }
                        ) {
                            // 横屏内容
                            content()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HorizontalWaveformContent(viewModel: WaveformViewModel = viewModel()) {
    val  waveformData by viewModel.waveformData.collectAsState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        RealTimeWaveformView(
            modifier = Modifier.fillMaxSize(),
            data = waveformData,
            lineColor = Color(0xFF00FF00), // 亮绿色，类似示波器
            backgroundColor = Color.Black
        )
    }
}
