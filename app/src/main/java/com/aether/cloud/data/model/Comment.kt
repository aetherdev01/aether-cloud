package com.aether.cloud.data.model

data class Comment(
    val id: String = "",
    val moduleId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhoto: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
