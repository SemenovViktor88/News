package com.semenov.news.core.db.entity

import androidx.room.Entity

@Entity(
    tableName = "articles",
    primaryKeys = ["contextKey", "url"],
)
data class ArticleEntity(
    val contextKey: String,
    val url: String,
    val title: String?,
    val description: String?,
    val imageUrl: String?,
    val publishedAt: String?,
    val sourceName: String?,
    val author: String?,
    val content: String?,
    val page: Int,
    val position: Int,
    val cachedAt: Long,
)
