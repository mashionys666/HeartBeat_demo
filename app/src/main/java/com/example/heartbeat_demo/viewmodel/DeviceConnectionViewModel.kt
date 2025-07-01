package com.example.heartbeat_demo.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.ui.input.key.type
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * DeviceConnectionViewModel handles Bluetooth device connection and communication.
 * 
 * Connection Management Approach:
 * - Uses standard Bluetooth GATT connection state tracking
 * - Does NOT implement automatic heartbeat packets
 * - Does NOT include periodic communication health checks
 * 
 * This design choice ensures:
 * - Lower power consumption
 * - Reduced network traffic
 * - Simplified connection state management
 * - Better compatibility with various devices
 * 
 * Connection status is reliable through GATT connection state callbacks.
 */
class DeviceConnectionViewModel(private val context: Context) :ViewModel(){
    private  val TAG = "DeviceConnectionViewModel"

    //bluetooth connect retry
    private val MAX_RETRIES = 3
    private val RETRY_DELAY_MS = 3000L // 3 seconds
    private var retryCount = 0

    //receive data——expose to UI
    private val _receivedData = MutableLiveData<String>()
    val receivedData: LiveData<String> = _receivedData

    // Callback interface——to audio raw_data
    interface DataCallback {
        fun onDataReceived(data: ByteArray)
    }
    // Callback instance
    var dataCallback: DataCallback? = null

    private  var bluetoothGatt : BluetoothGatt? = null
    private val bluetoothManager: BluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    // Bluetooth adapter
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }

    //Paired Devices
    private val _pairedDevices = MutableLiveData<List<BluetoothDevice>>()
    val pairedDevices: LiveData<List<BluetoothDevice>> = _pairedDevices

    private val _availableDevices = MutableLiveData<List<BluetoothDevice>>(emptyList())
    val availableDevices: LiveData<List<BluetoothDevice>> = _availableDevices

    // Scanning state
    private val _isScanning = MutableLiveData<Boolean>(false)
    val isScanning: LiveData<Boolean> = _isScanning

    //BlueTooth State
    private val _isBluetoothEnabled = MutableLiveData<Boolean>(false)
    val isBluetoothEnabled: LiveData<Boolean> = _isBluetoothEnabled

    // Connected device
    private val _connectedDevice = MutableLiveData<BluetoothDevice?>(null)
    val connectedDevice: LiveData<BluetoothDevice?> = _connectedDevice

    // Store the UUIDs
    private val _discoveredServices = MutableLiveData<Map<UUID, BluetoothGattService>>(emptyMap())
    val discoveredServices: LiveData<Map<UUID, BluetoothGattService>> = _discoveredServices

    private  val deviceList = mutableListOf<BluetoothDevice>()

    private  val connectedDevices = mutableListOf<BluetoothDevice>()
    // Store successfully connected devices

    private val _isConnected = MutableLiveData<Boolean>(false)
    val isConnected: LiveData<Boolean> = _isConnected


    private val scope = viewModelScope
    // Service and Characteristic UUIDs
    private val SERVICE_UUID = UUID.fromString(
        "a6ed0301-d344-460a-8075-b9e8ec90d71b")
    private val CHARACTERISTIC_UUID = UUID.fromString(
        "a6ed0302-d344-460a-8075-b9e8ec90d71b")
    private val DESCRIPTOR_UUID = UUID.fromString(
        "00002902-0000-1000-8000-00805f9b34fb")

    init {
        initializeBluetooth()
    }

    @SuppressLint("MissingPermission")
    fun initializeBluetooth() {
        val bluetoothAdapter = bluetoothManager.adapter
        _isBluetoothEnabled.value = bluetoothAdapter?.isEnabled ?: false
        if (bluetoothAdapter?.isEnabled == true) {
            getPairedDevices()
        }
    }

    @SuppressLint("MissingPermission")
     fun getPairedDevices() {
            if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
                ) {
                val pairedDevices = bluetoothManager.adapter.bondedDevices.toList()
                _pairedDevices.value = pairedDevices
            }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                bluetoothGatt?.disconnect()
                bluetoothGatt?.close()
                bluetoothGatt = null
                // Start new connection
                Log.d(TAG, "Initiating connection to device: ${device.address}")
                bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to device: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnectFromDevice() {
        Log.d(TAG, "disconnectFromDevice called")
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                bluetoothGatt?.disconnect()
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException during disconnect: ${e.message}")
            }
        } else {
            Log.e(TAG, "BLUETOOTH_CONNECT permission not granted, cannot disconnect")
        }
        _connectedDevice.value = null
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter!!.cancelDiscovery()
        }
        deviceList.clear()
        _availableDevices.value = emptyList()
        // Use BluetoothLeScanner for more control
        val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        if (bluetoothLeScanner != null) {
            // Configure scan settings for low latency
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // Aggressive scanning
                .build()

            // Start scanning with the new settings
            _isScanning.value = true
            bluetoothLeScanner.startScan(null, settings, scanCallback)

            // Stop scanning after 25 seconds
            scope.launch {
                delay(25000) // 25 seconds
                stopDiscovery()
            }
        } else {
            // Fallback to old method if BluetoothLeScanner is not available
            bluetoothAdapter?.startDiscovery()
            _isScanning.value = true
            scope.launch {
                delay(25000) // 25 seconds
                stopDiscovery()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        if (bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(scanCallback)
        } else {
            bluetoothAdapter?.cancelDiscovery()
        }
        _isScanning.value = false
    }

    private val gattCallback = object : BluetoothGattCallback() {

        @RequiresApi(Build.VERSION_CODES.O)
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            val currentTime = System.currentTimeMillis()
            Log.d(
                TAG,
                "onConnectionStateChange: status=$status, newState=$newState, time=$currentTime"
            )
            val deviceAddress = gatt?.device?.address

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "Connected to GATT server. Device address: $deviceAddress")
                    scope.launch(Dispatchers.Main) {
                        _connectedDevice.value = gatt?.device
                        _isConnected.value = true
                        
                        // Note: We do NOT start heartbeat here intentionally.
                        // Connection status is managed through GATT connection state only.
                        
                        // Set the preferred PHY to LE Coded PHY after connection
                        if (gatt?.device?.type == BluetoothDevice.DEVICE_TYPE_LE) {
                            Log.i(TAG, "LE Coded PHY is supported.")
                            // Set the preferred PHY to LE Coded PHY
                            val txPhy = BluetoothDevice.PHY_LE_CODED
                            val rxPhy = BluetoothDevice.PHY_LE_CODED
                            val phyOptions = BluetoothDevice.PHY_OPTION_S8 // Use S=8 for maximum range
                            val success = gatt.setPreferredPhy(txPhy, rxPhy, phyOptions)
                            Log.i(TAG, "setPreferredPhy() called, success: $success")
                        } else {
                            Log.i(TAG, "LE Coded PHY is not supported.")
                        }
                        //Discover services
                        val discoverSuccess = gatt?.discoverServices() ?: false
                        Log.d(TAG, "Service discovery initiated: $discoverSuccess")
                        if (!connectedDevices.contains(gatt!!.device)) {
                            connectedDevices.add(gatt.device)
                            _pairedDevices.value = connectedDevices.toList()
                        }
                    }
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "Disconnected from GATT server. Device address: $deviceAddress")
                    _connectedDevice.postValue(null)
                    _isConnected.postValue(false)
                    if (status == 133 && retryCount < MAX_RETRIES) {
                        retryCount++
                        scope.launch {
                            delay(RETRY_DELAY_MS)
                            Log.w(TAG, "Retrying connection attempt $retryCount")
                            connectToDevice(gatt!!.device)
                        }
                    } else {
                        retryCount = 0 // Reset retry count after failure or success
                    }
                }
            }
        }
        override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "PHY update successful: TX PHY = $txPhy, RX PHY = $rxPhy")
            } else {
                Log.e(TAG, "PHY update failed with status: $status")
            }
        }
        override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "PHY read successful: TX PHY = $txPhy, RX PHY = $rxPhy")
            } else {
                Log.e(TAG, "PHY read failed with status: $status")
            }
        }



        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            Log.d(TAG, "onServicesDiscovered: Services discovered!")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt?.getService(SERVICE_UUID)
                val characteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)
                if (characteristic != null) {
                    val characteristicProperties = characteristic.properties
                    Log.d(TAG, "Characteristic properties: $characteristicProperties")
                    val isNotify =
                        characteristicProperties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
                    if (isNotify) {
                        val setNotificationSuccess =
                            gatt.setCharacteristicNotification(characteristic, true)
                        Log.d(TAG, "Set characteristic notification: $setNotificationSuccess")
                        val descriptor = characteristic.getDescriptor(DESCRIPTOR_UUID)
                        if (descriptor != null) {
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            val writeDescriptorSuccess = gatt.writeDescriptor(descriptor)
                            Log.d(
                                TAG,
                                "Notification descriptor write initiated: $writeDescriptorSuccess")
                        }
                    }
                }
            }
            requestConnectionPriority(gatt)
            // Request MTU after services are discovered
            scope.launch(Dispatchers.Main) {
                delay(500) //Short delay before MTU request
                val mtuSuccess = gatt?.requestMtu(247) ?: false
                Log.d(TAG, "MTU request initiated: $mtuSuccess")
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onDescriptorWrite: Successfully wrote descriptor: ${descriptor?.uuid}")
            } else {
                Log.e(TAG, "onDescriptorWrite: Failed to write descriptor: ${descriptor?.uuid}, status: $status")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic
        ) {
            val data = characteristic.value
            if (data != null) {
//                val unsignedDecimalString = data.joinToString(", ") { (it.toInt() and 0xFF).toString() }
//                Log.e(TAG, "PACKET # $unsignedDecimalString")
                dataCallback?.onDataReceived(data)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            Log.d(TAG, "MTU changed - Size: $mtu, Status: $status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "MTU size successfully changed to: $mtu")
            } else {
                Log.e(TAG, "Failed to change MTU size. Status: $status")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            Log.d(TAG, "onCharacteristicRead - status: $status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val value = characteristic.value
                Log.d(TAG, "Read value: ${value?.joinToString("") { String.format("%02X", it) }}")
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            Log.d(TAG, "onCharacteristicWrite: status=$status, uuid=${characteristic.uuid}")
        }

    }
    @SuppressLint("MissingPermission")
    private fun requestConnectionPriority(gatt: BluetoothGatt?) {
        gatt?.let {
            // 请求高速连接模式
            val success = it.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
            Log.d(TAG, "Request connection priority HIGH: $success")
        }
    }


    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let { scanResult ->
                val device = scanResult.device
                val deviceName = device.name // Get the device name

                // Filter out devices with null or empty names (Unknown Device)
                if (!deviceName.isNullOrBlank()) {
                    Log.d(TAG, "onScanResult: Device: $deviceName, Address: ${device.address}")
                    if (!deviceList.contains(device)) {
                        deviceList.add(device)
                        _availableDevices.postValue(deviceList.toList())
                    } else {
                        Log.d(TAG, ("error add device"))
                    }
                } else {
                    Log.d(TAG, "onScanResult: Unknown Device found, Address: ${device.address}, skipping")
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.forEach { result ->
                val device = result.device
                val deviceName = device.name

                // Filter out devices with null or empty names (Unknown Device)
                if (!deviceName.isNullOrBlank()) {
                    Log.d(TAG, "onBatchScanResults: Device: $deviceName, Address: ${device.address}")
                    if (!deviceList.contains(device)) {
                        deviceList.add(device)
                        _availableDevices.postValue(deviceList.toList())
                    }
                } else {
                    Log.d(TAG, "onBatchScanResults: Unknown Device found, Address: ${device.address}, skipping")
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "onScanFailed: Scan failed with error code: $errorCode")
        }
    }

    /**
     * Clean-up method for the ViewModel.
     * Disconnects from Bluetooth device and closes GATT connection.
     * Note: No heartbeat cleanup needed as heartbeat functionality is not implemented.
     */
    @SuppressLint("MissingPermission")
    override fun onCleared() {
        super.onCleared()
        disconnectFromDevice()
        bluetoothGatt?.close()
    }
}

