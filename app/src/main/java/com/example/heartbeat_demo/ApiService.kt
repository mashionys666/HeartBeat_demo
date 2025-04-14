package com.example.heartbeat_demo

import com.google.android.gms.common.api.Response
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponseDto>

    @POST("auth/register")
    suspend fun register(@Body registerRequest: RegisterRequest): Response<RegisterResponseDto>

    @GET("auth/validate")
    suspend fun validateToken(@Header("Authorization") authToken: String): Response<UserDto>

    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") authToken: String): Response<Void>
}
// 登录请求
data class LoginRequest(
    val username: String,
    val password: String
)

// 登录响应
data class LoginResponseDto(
    val userId: String,
    val username: String,
    val email: String,
    val token: String,
    val avatarUrl: String?,
    val profile: String?
)

// 注册请求
data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String
)

// 注册响应
data class RegisterResponseDto(
    val userId: String,
    val username: String,
    val email: String,
    val token: String
)

// 用户 DTO
data class UserDto(
    val userId: String,
    val username: String,
    val email: String,
    val avatarUrl: String?,
    val profile: String?
)
object RetrofitClient {
    private const val BASE_URL = "https://your-api-url.com/api/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}