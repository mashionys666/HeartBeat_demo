package com.example.heartbeat_demo.Interface

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.heartbeat_demo.R
import com.example.heartbeat_demo.viewmodel.NavigationViewModel
import com.example.heartbeat_demo.viewmodel.UserManagementViewModel

@Composable
fun UserManagementUI(
    navController: NavHostController,
    navigationViewModel: NavigationViewModel,
    userManagementViewModel : UserManagementViewModel
) {
    AppScaffold(
        navController, navigationViewModel,
        drawerContent = {MyNavigationDrawer()}
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFE6F0FA)),// Light blue background
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Collect the isLoggedIn state from the ViewModel
            val isLoggedIn by userManagementViewModel.isLoggedIn.collectAsState()

            if(isLoggedIn) {
                PostLoginLayout(navController, userManagementViewModel)
            }else {
                PreLoginLayout(navController,userManagementViewModel)
            }
        }
    }
}

@Composable
fun PostLoginLayout(navController: NavHostController, userManagementViewModel: UserManagementViewModel) {
    Column (
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        // User Information Area
        UserInfoArea(userManagementViewModel)
        Spacer(modifier = Modifier.height(24.dp))
        // Menu Navigation Area
        MenuNavigationArea(navController, userManagementViewModel)
    }
}

@Composable
fun UserInfoArea(userManagementViewModel: UserManagementViewModel) {
    val currentUser by userManagementViewModel.currentUser.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFADD8E6), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // User Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                if (currentUser?.avatarUrl != null) {
                    // 使用 Coil 加载头像
                    AsyncImage(
                        model = currentUser?.avatarUrl,
                        contentDescription = "User Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.guanyu_us),
                        contentDescription = "User Icon",
                        tint = Color.Black,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Username and Profile
            Column {
                Text(
                    text = currentUser?.username ?: "Guest",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                )

                Text(
                    text = currentUser?.profile ?: "No Profile",
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}
@Composable
fun MenuNavigationArea(navController: NavHostController, userManagementViewModel: UserManagementViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFADD8E6), RoundedCornerShape(12.dp)) // Subtle blue border
            .padding(16.dp)
    ) {
        MenuItem(icon = painterResource(id = R.drawable.xiaoxi), text = " 消息中心")
        MenuItem(icon = painterResource(id = R.drawable.guanyu_us), text = "Text")
        MenuItem(icon = painterResource(id = R.drawable.bangzhu), text = "帮助反馈")
        MenuItem(icon = Icons.Filled.Info, text = "关于我们")
        MenuItem(icon = Icons.Filled.Settings, text = "Settings")
        MenuItem(
            icon = painterResource(id = R.drawable.shezhi3),
            text = "Logout",
            onClick = {
                userManagementViewModel.logout()
            }
        )
    }
}

@Composable
fun MenuItem(icon: Any, text: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ){
        when (icon) {
            is ImageVector -> Icon(
                imageVector = icon,
                contentDescription = text,
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )

            else -> Icon(
                painter = icon as Painter,
                contentDescription = text,
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, fontSize = 14.sp, color = Color.Black)
    }
}

@Composable
fun PreLoginLayout(navController: NavHostController, userManagementViewModel: UserManagementViewModel) {
    val isLoggedIn by userManagementViewModel.isLoggedIn.collectAsState()
    LaunchedEffect(key1 = !isLoggedIn) {
        if (!isLoggedIn) {
            navController.navigate("login") {
                popUpTo("main") { inclusive = true }
            }
        }
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = {
                // Navigate to the login screen
                navController.navigate("login")
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFADD8E6)) // Use ButtonDefaults from material3
        ) {
            Text(text = "Login", color = Color.Black)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UserManagementUIPreview() {
    // Create a dummy NavHostController for the preview
    val navController = NavHostController(LocalContext.current)
    val navigationViewModel = NavigationViewModel()
    val userManagementViewModel = UserManagementViewModel()
    UserManagementUI(navController, navigationViewModel, userManagementViewModel)
}