package com.example.heartbeat_demo.Interface

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.heartbeat_demo.DeviceConnectionViewModelFactory
import com.example.heartbeat_demo.viewmodel.DeviceConnectionViewModel
import com.example.heartbeat_demo.viewmodel.NavigationViewModel
import com.example.heartbeat_demo.viewmodel.UserManagementViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceConnectionUI(
    navController: NavHostController,
    navigationViewModel: NavigationViewModel,
    userManagementViewModel: UserManagementViewModel,
    deviceConnectionViewModel: DeviceConnectionViewModel = viewModel
        (factory = DeviceConnectionViewModelFactory(LocalContext.current))
) {
    val context = LocalContext.current
    val pairedDevices by deviceConnectionViewModel.pairedDevices.observeAsState(emptyList())
    val availableDevices by deviceConnectionViewModel.availableDevices.observeAsState(emptyList())
    val isScanning by deviceConnectionViewModel.isScanning.observeAsState(false)
    val isBluetoothEnabled by deviceConnectionViewModel.isBluetoothEnabled.observeAsState(false)
    val connectedDevice by deviceConnectionViewModel.connectedDevice.observeAsState()
    val discoveredServices by deviceConnectionViewModel.discoveredServices.observeAsState(emptyMap())

    // State to track if permissions are granted
    var permissionsGranted by remember { mutableStateOf(false) }

    //Permission
    val bluetoothPermissions = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            Log.d("DeviceConnectionUI", "permissionLauncher onResult called")
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                Log.d("DeviceConnectionUI", "All permissions granted")
                permissionsGranted = true
                deviceConnectionViewModel.initializeBluetooth()
            } else {
                Log.e("DeviceConnectionUI", "Permissions not granted")
                permissionsGranted = false
            }
        }
    )
    LaunchedEffect(key1 = true) {
        Log.d("DeviceConnectionUI", "LaunchedEffect started")
        val allPermissionsGranted = bluetoothPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        Log.d("DeviceConnectionUI", "allPermissionsGranted: $allPermissionsGranted")
        if (!allPermissionsGranted) {
            Log.d("DeviceConnectionUI", "Requesting permissions")
            permissionLauncher.launch(bluetoothPermissions)
            Log.d("DeviceConnectionUI", "permissionLauncher.launch called")
        } else {
            Log.d("DeviceConnectionUI", "Permissions already granted")
            permissionsGranted = true
            deviceConnectionViewModel.initializeBluetooth()
            Log.d("DeviceConnectionUI", "initializeBluetooth called")
        }
    }


    AppScaffold(
        navController,
        navigationViewModel,
       drawerContent = {MyNavigationDrawer()}
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF0F8FF))
                .verticalScroll(rememberScrollState())
        ) {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Device Connection",
                                textAlign = TextAlign.Center,
                                color = Color.Black
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                Icons.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.Black
                            )
                        }
                    },
                    colors = TopAppBarDefaults.smallTopAppBarColors(
                        containerColor = Color(0xFFADD8E6),
                    )
                )

                // Bluetooth Status
                BluetoothStatusSection(
                    isBluetoothEnabled = isBluetoothEnabled,
                    onBluetoothToggle = { enabled ->
                        if (enabled) {
                            deviceConnectionViewModel.getPairedDevices()
                        }
                    }
                )

                // Paired Devices
                PairedDevicesSection(
                    pairedDevices = pairedDevices,
                    onDeviceClick = { device ->
                        deviceConnectionViewModel.connectToDevice(device)
                    },
                    connectedDevice = connectedDevice
                )

                // Available Devices
                AvailableDevicesSection(
                    availableDevices = availableDevices,
                    isScanning = isScanning,
                    onScanToggle = { scanning ->
                        Log.d("DeviceConnectionUI", "permissionsGranted: $permissionsGranted")
                        if (permissionsGranted) {
                            if (scanning) {
                                Log.d("DeviceConnectionUI", "startDiscovery called")
                                deviceConnectionViewModel.startDiscovery()
                            } else {
                                deviceConnectionViewModel.stopDiscovery()
                            }
                        } else {
                            Log.e(
                                "DeviceConnectionUI",
                                "Permissions not granted, cannot start scan"
                            )
                        }
                    },
                    onDeviceClick = { device ->
                        deviceConnectionViewModel.connectToDevice(device)
                    },
                    connectedDevice = connectedDevice
                )

                //Discovered Services
                DiscoveredServicesSection(discoveredServices = discoveredServices)

            }
    }
}

@Composable
fun BluetoothStatusSection(isBluetoothEnabled: Boolean, onBluetoothToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "蓝牙", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = if (isBluetoothEnabled) "On" else "Off")
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = isBluetoothEnabled,
                onCheckedChange = onBluetoothToggle
            )
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun PairedDevicesSection(
    pairedDevices: List<BluetoothDevice>,
    onDeviceClick: (BluetoothDevice) -> Unit,
    connectedDevice: BluetoothDevice?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "已配对设备",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
        )
        Column( // Use Column with verticalScroll for independent scrolling
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp) // Set a max height for the section
                .verticalScroll(rememberScrollState())
        ){
            pairedDevices.forEach{ device ->
                DeviceItem(
                    deviceName = device.name ?: "Unknown Device",
                    isConnected = device == connectedDevice,
                    onDeviceClick = { onDeviceClick(device) }
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun AvailableDevicesSection(
    availableDevices: List<BluetoothDevice>,
    isScanning: Boolean,
    onScanToggle: (Boolean) -> Unit,
    onDeviceClick: (BluetoothDevice) -> Unit,
    connectedDevice: BluetoothDevice?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "可用设备",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = { onScanToggle(!isScanning) }) {
                Text(text = if (isScanning) "Stop Scan" else "Scan")
            }
        }
        if (isScanning) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
            }
        }
        Column( // Use Column with verticalScroll for independent scrolling
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
                .verticalScroll(rememberScrollState())
        ) {
            availableDevices.forEach { device -> // Use forEach instead of LazyColumn
                DeviceItem(
                    deviceName = device.name ?: "Unknown Device",
                    isConnected = device == connectedDevice,
                    onDeviceClick = { onDeviceClick(device) }
                )
            }
        }
    }
}

@Composable
fun DiscoveredServicesSection(discoveredServices: Map<UUID, BluetoothGattService>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Discovered Services",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
        )
        if (discoveredServices.isEmpty()) {
            Text(
                text = "No services discovered yet.",
                modifier = Modifier.padding(16.dp)
            )
        } else {
            Column(
                                modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 200.dp) // Set a max height for the section
                                            .verticalScroll(rememberScrollState()) // Add verticalScroll here
                                    ) {
                discoveredServices.forEach { (uuid, service) -> // Changed: Use forEach instead of items
                    ServiceItem(serviceName = "Service", serviceUUID = uuid.toString())
                }
            }
        }
    }
}

@Composable
fun ServiceItem(serviceName: String, serviceUUID: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$serviceName: $serviceUUID",
            fontSize = 16.sp,
        )
    }
}

@Composable
fun DeviceItem(deviceName: String, isConnected: Boolean = false, onDeviceClick: () -> Unit = {}) {
    Log.d("DeviceItem", "Device: $deviceName, isConnected: $isConnected")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDeviceClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = deviceName,
            fontSize = 16.sp,
            color = if (isConnected) Color.Green else Color.Black
        )
        if (isConnected) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "(Connected)", color = Color.Green)
        }
    }
}

