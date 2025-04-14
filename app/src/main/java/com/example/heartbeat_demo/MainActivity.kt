package com.example.heartbeat_demo

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.heartbeat_demo.Interface.DataInterfaceUI
import com.example.heartbeat_demo.Interface.DeviceConnectionUI
import com.example.heartbeat_demo.Interface.HomePageUI
import com.example.heartbeat_demo.Interface.UserLoginUI
import com.example.heartbeat_demo.Interface.UserManagementUI
import com.example.heartbeat_demo.ui.theme.HeartBeat_demoTheme
import com.example.heartbeat_demo.viewmodel.DataInterfaceViewModelFactory
import com.example.heartbeat_demo.viewmodel.DeviceConnectionViewModel
import com.example.heartbeat_demo.viewmodel.HomePageViewModel
import com.example.heartbeat_demo.viewmodel.NavigationItem
import com.example.heartbeat_demo.viewmodel.NavigationViewModel
import com.example.heartbeat_demo.viewmodel.UserLoginViewModel
import com.example.heartbeat_demo.viewmodel.UserManagementViewModel
import com.example.heartbeat_demo.viewmodel.dataInterfaceViewModel

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HeartBeat_demoTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp()
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun  MainApp() {
    val navController = rememberNavController()
    val userManagementViewModel = UserManagementViewModel()
    val navigationViewModel = NavigationViewModel()
    val userLoginViewModel = UserLoginViewModel(userManagementViewModel)
    val homePageViewModel = HomePageViewModel()
    val deviceConnectionViewModel: DeviceConnectionViewModel = viewModel(factory = DeviceConnectionViewModelFactory(LocalContext.current))


    NavHost(navController = navController, startDestination = NavigationItem.USER_MANAGEMENT.name) {

        composable("login") {
            UserLoginUI(userLoginViewModel, navController)
        }
        composable(NavigationItem.DATA_MANAGEMENT.name) {
            if(userManagementViewModel.checkLoginStatus()){
                HomePageUI(navController,navigationViewModel, homePageViewModel)
            }else {
                Toast.makeText(LocalContext.current, "You are not logged in", Toast.LENGTH_SHORT).show()
            }
        }
        composable(NavigationItem.USER_MANAGEMENT.name) {
            UserManagementUI(navController, navigationViewModel, userManagementViewModel)
        }
        composable(NavigationItem.DEVICE_CONNECTION.name) {
            if (userManagementViewModel.checkLoginStatus()) {
                DeviceConnectionUI(navController, navigationViewModel, userManagementViewModel,deviceConnectionViewModel)
            } else {
                Toast.makeText(LocalContext.current, "You are not logged in", Toast.LENGTH_SHORT).show()
            }
        }
        composable(
            route = "data_interface/{modeName}",
            arguments = listOf(navArgument("modeName") { type =
                NavType.StringType })
        ) { backStackEntry ->
            val modeName = backStackEntry.arguments?.getString("modeName") ?: "Normal"
            val dataInterfaceViewModel: dataInterfaceViewModel = viewModel(factory = DataInterfaceViewModelFactory(LocalContext.current, deviceConnectionViewModel, modeName))
            DataInterfaceUI(navController,navigationViewModel,dataInterfaceViewModel,deviceConnectionViewModel)
        }
    }
}

// ViewModel Factory
class DeviceConnectionViewModelFactory(private val context: Context) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeviceConnectionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DeviceConnectionViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}