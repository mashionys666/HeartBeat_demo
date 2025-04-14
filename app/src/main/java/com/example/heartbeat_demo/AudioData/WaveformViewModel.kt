package com.example.heartbeat_demo.AudioData

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

class WaveformViewModel : ViewModel() {
    //缓冲区大小
    private val bufferSize = 1024
    // 波形数据
    private val  _waveformData = MutableStateFlow(ByteArray(0))
     val waveformData: StateFlow<ByteArray> = _waveformData

    //add new data to waveform
    fun addData(newData: ByteArray) {
        val currentData = _waveformData.value
        val combinedSize = currentData.size + newData.size
        val trimmedData = if (combinedSize > bufferSize) {
            val start = combinedSize - bufferSize
            val temp = ByteArray(bufferSize)
            if (start < currentData.size) {
                currentData.copyInto(temp, 0, start)
                newData.copyInto(temp, currentData.size - start)
            } else {
                newData.copyInto(temp, 0, start - currentData.size)
            }
            temp
        } else {
            val temp = ByteArray(combinedSize)
            currentData.copyInto(temp)
            newData.copyInto(temp, currentData.size)
            temp
        }
        _waveformData.value = trimmedData
    }

    fun clearData() {
        _waveformData.value = ByteArray(0)
    }
}

@Composable
fun RealTimeWaveformView(
    modifier: Modifier = Modifier,
    data: ByteArray,
    maxDataPoints: Int = 1000,
    lineColor: Color = Color(0xFF4CAF50),
    backgroundColor: Color = Color(0xFF102027),
    waveformThickness: Dp = 2.dp,
    centerLineColor: Color = lineColor.copy(alpha = 0.3f),
    centerLineThickness: Dp = 1.dp,
    centerLineDashLength: Dp = 5.dp,
    centerLineDashGap: Dp = 5.dp,
    waveformPadding: Dp = 8.dp,
    scalingFactor: Float = 0.9f
) {
    val  displayableData = remember (data){
        // 如果数据点过多，只取最新的maxDataPoints个点
        if(data.size> maxDataPoints){
            data.takeLast(maxDataPoints).toByteArray()
        }else{
            data
        }
    }
    Box(
        modifier = modifier
            .background(backgroundColor)
            .padding(4.dp)
    ){
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val middleY = height / 2

            if (displayableData.isEmpty()) return@Canvas // Nothing to draw

            // Calculate the maximum absolute value in the data for scaling
            val maxValue = displayableData.maxOfOrNull { abs(it.toInt()) } ?: 1
            // Calculate the scaling factor based on the maximum value
            val scalingFactor = (middleY * 0.9f) / maxValue

            // Calculate the horizontal step between data points
            val xStep = if (displayableData.size > 1) {
                width / (displayableData.size - 1)
            } else {
                0f // No step if only one or zero data points
            }

            val path = Path()
            // Move to the starting point
            path.moveTo(0f, middleY)

            // Draw the waveform line
            for (i in displayableData.indices) {
                val x = i * xStep
                // Normalize the byte data to a range based on the scaling factor
                val normalizedValue = displayableData[i].toFloat() * scalingFactor / 128f
                val y = middleY - normalizedValue

                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            // Draw the waveform path
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw the center line
            drawLine(
                color = lineColor.copy(alpha = 0.3f),
                start = Offset(0f, middleY),
                end = Offset(width, middleY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx()))
            )
        }
    }
}

fun  createDummyWaveformData(size: Int = 1000) : ByteArray {
    val dummyData = ByteArray(size)
    for (i in 0 until size) {
        val value = (kotlin.math.sin(i * 0.1) * 127).toInt().toByte()
        dummyData[i] = value
    }
    return dummyData
}

@Composable
fun WaveformDisplayScreen() {
    val dummyData = createDummyWaveformData(500)

    Column {
        RealTimeWaveformView(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            data = dummyData,
            lineColor = Color.Green,
            backgroundColor = Color.DarkGray,
            waveformThickness = 3.dp,
            centerLineColor = Color.Yellow.copy(alpha = 0.5f),
            centerLineThickness = 2.dp,
            centerLineDashLength = 10.dp,
            centerLineDashGap = 5.dp
        )
    }
}
