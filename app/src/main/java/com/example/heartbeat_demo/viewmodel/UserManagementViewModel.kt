package com.example.heartbeat_demo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heartbeat_demo.User
import com.example.heartbeat_demo.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserManagementViewModel :ViewModel(){

    // Track the login status
    private  val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    //当前用户信息
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    // 登录加载状态
    private val _loginLoading = MutableStateFlow(false)
    val loginLoading: StateFlow<Boolean> = _loginLoading.asStateFlow()
    // 错误信息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    // 检查是否有已保存的登录
    init {
        // 从 SharedPreferences 读取登录状态
        viewModelScope.launch {
            checkSavedLogin()
        }
    }

    fun  login(username: String, password: String) {
        viewModelScope.launch {
            try {
                _loginLoading.value = true
                _errorMessage.value = null

                // 调用后端 API 进行登录
                val response = UserRepository.login(username, password)

                if (response.isSuccessful) {
                    // 登录成功，保存用户信息和 token
                    val user = response.user
                    val token = response.token

                    // 保存到 SharedPreferences
                    saveUserLoginData(user, token)

                    _currentUser.value = user
                    _isLoggedIn.value = true
                } else {
                    // 登录失败
                    _errorMessage.value = response.errorMessage ?: "登录失败，请检查用户名和密码"
                }
            } catch (e: Exception) {
                _errorMessage.value = "连接错误：${e.localizedMessage}"
            } finally {
                _loginLoading.value = false
            }
        }
    }
    // 注册函数
    fun register(username: String, password: String, email: String) {
        viewModelScope.launch {
            try {
                _loginLoading.value = true
                _errorMessage.value = null

                // 调用后端 API 进行注册
                val response = UserRepository.register(username, password, email)

                if (response.isSuccessful) {
                    // 注册成功，可以自动登录
                    val user = response.user
                    val token = response.token

                    // 保存到 SharedPreferences
                    saveUserLoginData(user, token)

                    _currentUser.value = user
                    _isLoggedIn.value = true
                } else {
                    // 注册失败
                    _errorMessage.value = response.errorMessage ?: "注册失败，请稍后再试"
                }
            } catch (e: Exception) {
                _errorMessage.value = "连接错误：${e.localizedMessage}"
            } finally {
                _loginLoading.value = false
            }
        }
    }
    fun logout() {
        viewModelScope.launch {
            // 清除本地保存的登录信息
            clearUserLoginData()

            // 调用后端登出 API（如果有的话）
            try {
                UserRepository.logout()
            } catch (e: Exception) {
                // 即使后端登出失败，我们仍然在本地进行登出
                Log.e("UserManagementViewModel", "后端登出失败", e)
            }

            // 重置状态
            _currentUser.value = null
            _isLoggedIn.value = false
        }
    }
    // 检查保存的登录信息
    private suspend fun checkSavedLogin() {
        withContext(Dispatchers.IO) {
            val sharedPrefs = MyApplication.instance.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val token = sharedPrefs.getString("auth_token", null)

            if (token != null) {
                // 有保存的 token，尝试验证
                try {
                    val response = UserRepository.validateToken(token)

                    if (response.isSuccessful) {
                        // token 有效，设置登录状态
                        _currentUser.value = response.user
                        _isLoggedIn.value = true
                    } else {
                        // token 无效，清除
                        clearUserLoginData()
                    }
                } catch (e: Exception) {
                    // 连接错误，但我们不显示给用户
                    Log.e("UserManagementViewModel", "验证 token 失败", e)
                }
            }
        }
    }
    // 保存用户登录信息
    private fun saveUserLoginData(user: User, token: String) {
        val sharedPrefs = MyApplication.instance.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putString("auth_token", token)
            putString("user_id", user.id)
            putString("username", user.username)
            apply()
        }
    }

    // 清除用户登录信息
    private fun clearUserLoginData() {
        val sharedPrefs = MyApplication.instance.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().apply()
    }
    // Check if the user is logged in
    fun checkLoginStatus(): Boolean {
        return _isLoggedIn.value
    }
    // Function to update the login status
    fun updateLoginStatus(isLoggedIn: Boolean) {
        viewModelScope.launch {
            _isLoggedIn.value = isLoggedIn
        }
    }
}