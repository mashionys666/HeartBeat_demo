package com.example.heartbeat_demo.AudioData

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.filters.BandPass
import be.tarsos.dsp.filters.HighPass
import be.tarsos.dsp.filters.LowPassFS
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import com.android.identity.util.Logger.isDebugEnabled
import com.example.heartbeat_demo.viewmodel.DeviceConnectionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.abs
import kotlin.math.sin


class  AudioPlaybackManager : DeviceConnectionViewModel.DataCallback
{
    private val  TAG  = "AudioPlaybackManager"
    private var audioTrack: AudioTrack? = null
    private  var  playbackJob: Job? = null
    private  val playbackScope = CoroutineScope(Dispatchers.IO)

    // PCM format
    private val SAMPLE_RATE = 12000
    //CHANNEL_OUT_STEREO  (立体声)    CHANNEL_OUT_MONO(单通道)
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT   //Audio format: 16-bit PCM
    private val TARGET_LATENCY_MS = 200     //目标延迟毫秒数
    // LiveData to expose PCM data to the UI
    private  val _pcmData = MutableLiveData<ByteArray>()
    val pcmData : LiveData<ByteArray> = _pcmData

    // 单环形缓冲区配置
    private val bufferSizeBytes = (SAMPLE_RATE * 2 * TARGET_LATENCY_MS / 1000).coerceAtLeast(4096)
    private val circularBuffer = ByteArray(bufferSizeBytes)
    private val bufferLock = ReentrantLock()
    private var writePosition = 0
    private var readPosition = 0
    private var bytesAvailable = AtomicInteger(0)

    //滤波器
    private var isNoiseEstimated = false
    private val noiseEstimationBuffer = ShortArray(SAMPLE_RATE / 3) // 0.3秒噪声样本
    private var noiseBufferIndex = 0
    private var filterEnabled = true
    // TarsosDSP 相关成员
    private val audioFormat = TarsosDSPAudioFormat(SAMPLE_RATE.toFloat(), 16, 1, true, false)
    private val audioConverter by lazy {
        try {
            // 尝试获取正确的转换器
            TarsosDSPAudioFloatConverter.getConverter(audioFormat)
        } catch (e: Exception) {
            Log.e(TAG, "获取音频转换器失败: ${e.message}", e)
            null
        }
    }

    // 音频处理链 - 心音优化
    private val heartSoundBandpass = BandPass(20f, 200f, SAMPLE_RATE.toFloat())
    private val lowPassFilter = LowPassFS(400f, SAMPLE_RATE.toFloat()) // 平滑高频
    private val highPassFilter = HighPass(15f, SAMPLE_RATE.toFloat())  // 去除极低频噪音


    init {
        initializeAudioTrack()
    }

    private fun initializeAudioTrack() {
        try {
            val  minBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )
            // 确保AudioTrack缓冲区足够大但不引入过多延迟
            val trackBufferSize = (minBufferSize * 1.5).toInt().coerceAtLeast(bufferSizeBytes)
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AUDIO_FORMAT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_CONFIG)
                        .build()
                )
                .setBufferSizeInBytes(trackBufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            // 确认 AudioTrack初始化成功
            if (audioTrack?.state == AudioTrack.STATE_INITIALIZED) {
                Log.d(TAG, "AudioTrack初始化成功")
            } else {
                Log.e(TAG, "初始化AudioTrack时出错")
            }
        }catch (e: Exception) {
           Log.e(TAG,"初始化AudioTrack失败")
        }
    }

    @SuppressLint("RestrictedApi")
    fun startPlayback() {
        Log.d(TAG, "开始播放，检查状态....")
        if (playbackJob?.isActive == true) return

        // // 重置AudioTrack确保干净的开始
        try {
            audioTrack?.stop()
            audioTrack?.release()
            initializeAudioTrack()
        } catch (e: Exception) {
            Log.e(TAG, "重置AudioTrack时出错", e)
        }
        // 播放简短的测试音以验证系统并提供用户反馈
        playStartupBeep()
        playbackJob = playbackScope.launch {
            try {
                audioTrack?.play()
                val playState = audioTrack?.playState
                while (isActive){
                    // 检查是否有数据可读
                    val availableBytes = bytesAvailable.get()
                    if (availableBytes > 0) {
                        val bytesToRead = minOf(availableBytes, 2048)  // 限制每次读取量以降低延迟
                        val audioData = ByteArray(bytesToRead)
                        // 从环形缓冲区安全读取
                        bufferLock.withLock {
                            if (readPosition + bytesToRead <= bufferSizeBytes) {
                                // 单次读取
                                circularBuffer.copyInto(audioData, 0, readPosition, readPosition + bytesToRead)
                            } else {
                                // 环绕读取 - 分两部分
                                val firstPartSize = bufferSizeBytes - readPosition
                                circularBuffer.copyInto(audioData, 0, readPosition, bufferSizeBytes)
                                circularBuffer.copyInto(audioData, firstPartSize, 0, bytesToRead - firstPartSize)
                            }

                            // 更新读取位置
                            readPosition = (readPosition + bytesToRead) % bufferSizeBytes

                            // 更新可用字节数
                            bytesAvailable.addAndGet(-bytesToRead)
                        }
                        // 通知UI更新波形
                        withContext(Dispatchers.Main) {
                            _pcmData.value = audioData
                        }
                        // 检查音频数据是否有效
                        val audioLevel = calculateAudioLevel(audioData)
                        if (isDebugEnabled && audioData.size >= 10) {
                            Log.d(TAG,"音频数据: 大小=${audioData.size}, 电平=$audioLevel, 前10字节=${
                                audioData.take(10).joinToString { "%02X".format(it) }}")
                        }
                        // 写入AudioTrack播放
                        val result = audioTrack?.write(audioData, 0, audioData.size) ?: 0

                        if (result < 0) {
                            Log.e(TAG, "AudioTrack.write()错误: $result")
                        } else if (result != audioData.size) {
                            Log.w(TAG, "AudioTrack.write()部分写入: $result/${audioData.size}字节")
                        }
                    } else {
                        // 无数据可播放，短暂等待
                        delay(10) // 10ms
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "播放循环中发生错误", e)
            } finally {
                Log.d(TAG,"播放循环结束")
            }
        }
    }

    override  fun  onDataReceived(data: ByteArray) {
//        val unsignedDecimalString = data.joinToString(", ") { (it.toInt() and 0xFF).toString() }
//        Log.e(TAG, "PACKET # $unsignedDecimalString")
        hexToPcm(data)
    }

    private fun hexToPcm(hexData: ByteArray) {
        try {
            //验证数据有效性
            if (hexData.size %2 !=0) {
                Log.e(TAG, "hexToPcm: 无效数据大小 (${hexData.size})，需为偶数")
                return
            }
            //分配PCM处理数组
            val pcmData = ShortArray(hexData.size / 2)
            var pcmIndex = 0
            // 计算最大振幅用于动态增益调整
            var maxAmplitude = 0
            for (i in 0 until hexData.size step 2) {
                if (i + 1 >= hexData.size) break  // 安全检查

                // 大端序解析
                val unsignedSample = ((hexData[i].toInt() and 0xFF) shl 8) or
                        (hexData[i + 1].toInt() and 0xFF)
                val signedSample = unsignedSample - 32768
                maxAmplitude = maxOf(maxAmplitude, abs(signedSample))
            }
            // 基于最大振幅计算增益因子 - 确保足够音量但避免过度放大噪音
            val gainFactor = when {
                maxAmplitude > 5000 -> 1.2f  // 信号强，轻微增益
                maxAmplitude > 1000 -> 2.0f  // 中等信号，中等增益
                maxAmplitude > 100 -> 4.0f   // 弱信号，强增益
                else -> 1.0f                 // 几乎无信号或静音，不增益
            }
            // 应用增益并转换数据
            for (i in 0 until hexData.size step 2) {
                if (i + 1 >= hexData.size) break  // 安全检查

                // 大端序解析
                val unsignedSample = ((hexData[i].toInt() and 0xFF) shl 8) or
                        (hexData[i + 1].toInt() and 0xFF)

                // 转换为有符号值并应用增益
                val signedSample = unsignedSample - 32768
                val amplifiedSample = (signedSample * gainFactor).toInt()

                // 限幅避免失真
                val clippedSample = maxOf(-32768, minOf(32767, amplifiedSample))

                pcmData[pcmIndex++] = clippedSample.toShort()
            }
            //转换为字节数组
            val byteData = pcmData.toByteArray()
            //写入环形缓冲区
            // 写入环形缓冲区
            writeToCircularBuffer(byteData)
        } catch (e: Exception) {
            Log.e(TAG, "hexToPcm处理异常", e)
        }
    }
    private fun hexToPcmWithFiltering(hexData: ByteArray) {
        try {
            // 验证数据有效性
            if (hexData.size % 2 != 0) {
                Log.e(TAG, "hexToPcm: 无效数据大小 (${hexData.size})，需为偶数")
                return
            }

            // 分配PCM处理数组
            val pcmData = ShortArray(hexData.size / 2)
            var pcmIndex = 0

            // 第一步：转换为PCM数据
            for (i in 0 until hexData.size step 2) {
                if (i + 1 >= hexData.size) break  // 安全检查

                // 大端序解析
                val unsignedSample = ((hexData[i].toInt() and 0xFF) shl 8) or
                        (hexData[i + 1].toInt() and 0xFF)
                val signedSample = unsignedSample - 32768

                pcmData[pcmIndex++] = signedSample.toShort()
            }

            // 应用TarsosDSP处理
            val processedData = if (filterEnabled) {
                processPcmWithTarsosDSP(pcmData)
            } else {
                pcmData
            }

            // 动态增益（保持与您原来的增益处理逻辑类似）
            val maxAmplitude = processedData.maxOfOrNull { abs(it.toInt()) } ?: 0
            val gainFactor = when {
                maxAmplitude > 5000 -> 1.2f  // 信号强，轻微增益
                maxAmplitude > 1000 -> 2.0f  // 中等信号，中等增益
                maxAmplitude > 100 -> 4.0f   // 弱信号，强增益
                else -> 1.0f                 // 几乎无信号或静音，不增益
            }

            // 应用增益
            val finalData = if (gainFactor != 1.0f) {
                ShortArray(processedData.size) { i ->
                    val amplifiedSample = (processedData[i] * gainFactor).toInt()
                    maxOf(-32768, minOf(32767, amplifiedSample)).toShort()
                }
            } else {
                processedData
            }

            // 转换为字节数组
            val byteData = finalData.toByteArray()

            // 写入环形缓冲区
            writeToCircularBuffer(byteData)
        } catch (e: Exception) {
            Log.e(TAG, "hexToPcm处理异常", e)
            e.printStackTrace()
        }
    }
    // 使用TarsosDSP处理PCM数据
    private fun processPcmWithTarsosDSP(pcmData: ShortArray): ShortArray {
        try {
            // 如果数据长度太短或转换器初始化失败，直接返回原始数据
            if (pcmData.size < 10 || audioConverter == null) {
                return pcmData
            }

            // 创建浮点数组
            val floatBuffer = FloatArray(pcmData.size)

            // 手动转换Short到Float (范围从-32768~32767到-1.0~1.0)
            for (i in pcmData.indices) {
                floatBuffer[i] = pcmData[i] / 32768.0f
            }

            // 创建音频事件
            val audioEvent = AudioEvent(audioFormat)
            audioEvent.floatBuffer = floatBuffer

            // 应用滤波器处理
            try {
                // 高通滤波器去除极低频噪音
                highPassFilter.process(audioEvent)

                // 心音带通滤波器
                heartSoundBandpass.process(audioEvent)

                // 低通滤波器平滑处理
                lowPassFilter.process(audioEvent)
            } catch (e: Exception) {
                Log.e(TAG, "应用滤波器出错: ${e.message}", e)
            }

            // 手动转换Float回Short
            val processedData = ShortArray(pcmData.size)
            for (i in floatBuffer.indices) {
                // 将-1.0~1.0范围转回-32768~32767
                val shortValue = (floatBuffer[i] * 32767).toInt().coerceIn(-32768, 32767).toShort()
                processedData[i] = shortValue
            }

            return processedData
        } catch (e: Exception) {
            Log.e(TAG, "TarsosDSP处理出错: ${e.message}", e)
            e.printStackTrace()
            // 出错时返回原始数据
            return pcmData
        }
    }


    private fun writeToCircularBuffer(data: ByteArray) {
        if (data.isEmpty()) return
        try {
            bufferLock.withLock {
                // 检查缓冲区是否有足够空间
                val freeSpace = bufferSizeBytes - bytesAvailable.get()
                if (data.size > freeSpace) {
                    //Log.e(TAG,"缓冲区空间不足: 需要${data.size}字节, 可用${freeSpace}字节")
                    // 更新读取位置，丢弃部分旧数据
                    val bytesToDiscard = data.size - freeSpace
                    readPosition = (readPosition + bytesToDiscard) % bufferSizeBytes
                    bytesAvailable.addAndGet(-bytesToDiscard)
                }
                // 写入新数据
                if (writePosition + data.size <= bufferSizeBytes) {
                    // 单次写入
                    data.copyInto(circularBuffer, writePosition)
                    writePosition = (writePosition + data.size) % bufferSizeBytes
                } else {
                    // 环绕写入 - 分两部分
                    val firstPartSize = bufferSizeBytes - writePosition
                    data.copyInto(circularBuffer, writePosition, 0, firstPartSize)
                    data.copyInto(circularBuffer, 0, firstPartSize, data.size)
                    writePosition = data.size - firstPartSize
                }
                //更新可用字节数
                bytesAvailable.addAndGet(data.size)
                //Log.d(TAG,"写入${data.size}字节到环形缓冲区, 当前可用${bytesAvailable.get()}/${bufferSizeBytes}字节")
            }
        }catch (e: Exception) {
            Log.e(TAG, "写入环形缓冲区时出错", e)
        }
    }

    fun pausePlayback() {
        audioTrack?.pause()
        Log.d(TAG,"音频播放已暂停")
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        audioTrack?.stop()
        // 重置缓冲区状态
        bufferLock.withLock {
            readPosition = 0
            writePosition = 0
            bytesAvailable.set(0)
        }
        Log.d(TAG,"音频已停止")
    }

    // 扩展函数：ShortArray转ByteArray (小端序)
    private fun ShortArray.toByteArray(): ByteArray {
        val byteArray = ByteArray(this.size * 2)
        for (i in this.indices) {
            // 低字节在前 (小端序)
            byteArray[i * 2] = (this[i].toInt() and 0xFF).toByte()
            byteArray[i * 2 + 1] = (this[i].toInt() shr 8 and 0xFF).toByte()
        }
        return byteArray
    }
    // 播放启动提示音以验证音频系统工作正常
    private fun playStartupBeep() {
        try {
            val beepDuration = 0.1  // 秒
            val samples = (SAMPLE_RATE * beepDuration).toInt()
            val beepData = ShortArray(samples)

            // 生成短促的880Hz提示音（心音应用的友好提示音）
            for (i in 0 until samples) {
                // 添加淡入淡出以避免爆音
                val amplitude = when {
                    i < samples / 10 -> (i * 10 * 16000 / samples)  // 淡入
                    i > samples * 9 / 10 -> ((samples - i) * 10 * 16000 / samples)  // 淡出
                    else -> 16000  // 持续
                }

                beepData[i] = (sin(2.0 * Math.PI * 880 * i / SAMPLE_RATE) * amplitude).toInt().toShort()
            }

            val beepBytes = beepData.toByteArray()
            audioTrack?.play()
            audioTrack?.write(beepBytes, 0, beepBytes.size)

            Log.d(TAG,"播放启动提示音 (${beepBytes.size}字节)")
        } catch (e: Exception) {
            Log.e(TAG, "播放启动提示音时出错", e)
        }
    }
    // 计算音频电平 (用于调试和UI反馈)
    private fun calculateAudioLevel(data: ByteArray): Double {
        if (data.size < 4) return 0.0

        var sum = 0.0
        var count = 0

        for (i in 0 until data.size step 2) {
            if (i + 1 >= data.size) break

            // 小端序解析 (播放用格式)
            val sample = (data[i].toInt() and 0xFF) or ((data[i+1].toInt() and 0xFF) shl 8)
            sum += abs(sample)
            count++
        }

        return if (count > 0) sum / count else 0.0
    }

    fun release() {
        stopPlayback()
        audioTrack?.release()
        audioTrack = null
        playbackScope.cancel()
    }
}



