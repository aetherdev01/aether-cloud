package com.aether.cloud.data.model

data class Module(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhoto: String = "",
    val name: String = "",
    val description: String = "",
    val version: String = "",
    val type: String = "NO_ROOT",
    val fileUrl: String = "",
    val fileSize: Long = 0,
    val mirrorUrl: String = "",
    val screenshots: List<String> = emptyList(),
    val downloadCount: Long = 0,
    val viewCount: Long = 0,
    val commentCount: Long = 0,
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
