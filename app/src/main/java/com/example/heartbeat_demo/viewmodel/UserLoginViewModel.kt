package com.example.heartbeat_demo.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

class UserLoginViewModel( private val userManagementViewModel: UserManagementViewModel) : ViewModel() {

    var username by mutableStateOf("")
    var password by mutableStateOf("")

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    fun login() {
        _loginState.value = LoginState.Loading
        // 模拟登录过程
        if (username == "admin" && password == "123456") {
            _loginState.value = LoginState.Success
            // Update the login status in UserManagementViewModel
            userManagementViewModel.updateLoginStatus(true)
        } else {
            _loginState.value = LoginState.Error("用户名或密码错误")
        }
    }
}