package com.aether.cloud.data.model

data class User(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val telegramChannel: String = "",
    val githubUrl: String = "",
    val website: String = "",
    val isDeveloper: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
