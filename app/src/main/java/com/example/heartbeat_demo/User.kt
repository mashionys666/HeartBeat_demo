package com.example.heartbeat_demo

data class User(
    val id: String,
    val username: String,
    val email: String,
    val avatarUrl: String? = null,
    val profile: String? = null
)

data class LoginResponse(
    val isSuccessful: Boolean,
    val user: User? = null,
    val token: String? = null,
    val errorMessage: String? = null
)

data class RegisterResponse(
    val isSuccessful: Boolean,
    val user: User? = null,
    val token: String? = null,
    val errorMessage: String? = null
)