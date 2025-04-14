package com.example.heartbeat_demo

import android.util.Log

object UserRepository {
    // API 客户端
    private val apiClient = RetrofitClient.apiService

    // 本地存储的 token
    private var authToken: String? = null

    // 初始化 token
    init {
        val sharedPrefs = MyApplication.instance.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        authToken = sharedPrefs.getString("auth_token", null)
    }

    // 登录方法
    suspend fun login(username: String, password: String): LoginResponse {
        return try {
            val response = apiClient.login(LoginRequest(username, password))

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                authToken = body.token

                LoginResponse(
                    isSuccessful = true,
                    user = User(
                        id = body.userId,
                        username = body.username,
                        email = body.email,
                        avatarUrl = body.avatarUrl,
                        profile = body.profile
                    ),
                    token = body.token
                )
            } else {
                LoginResponse(
                    isSuccessful = false,
                    errorMessage = response.errorBody()?.string() ?: "登录失败"
                )
            }
        } catch (e: Exception) {
            LoginResponse(
                isSuccessful = false,
                errorMessage = e.localizedMessage ?: "连接错误"
            )
        }
    }

    // 注册方法
    suspend fun register(username: String, password: String, email: String): RegisterResponse {
        return try {
            val response = apiClient.register(RegisterRequest(username, password, email))

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                authToken = body.token

                RegisterResponse(
                    isSuccessful = true,
                    user = User(
                        id = body.userId,
                        username = body.username,
                        email = body.email
                    ),
                    token = body.token
                )
            } else {
                RegisterResponse(
                    isSuccessful = false,
                    errorMessage = response.errorBody()?.string() ?: "注册失败"
                )
            }
        } catch (e: Exception) {
            RegisterResponse(
                isSuccessful = false,
                errorMessage = e.localizedMessage ?: "连接错误"
            )
        }
    }

    // 验证 token 方法
    suspend fun validateToken(token: String): LoginResponse {
        return try {
            val response = apiClient.validateToken("Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                LoginResponse(
                    isSuccessful = true,
                    user = User(
                        id = body.userId,
                        username = body.username,
                        email = body.email,
                        avatarUrl = body.avatarUrl,
                        profile = body.profile
                    ),
                    token = token
                )
            } else {
                LoginResponse(isSuccessful = false)
            }
        } catch (e: Exception) {
            LoginResponse(isSuccessful = false)
        }
    }

    // 登出方法
    suspend fun logout() {
        if (authToken != null) {
            try {
                apiClient.logout("Bearer $authToken")
            } catch (e: Exception) {
                Log.e("UserRepository", "登出 API 调用失败", e)
            }

            authToken = null
        }
    }
}