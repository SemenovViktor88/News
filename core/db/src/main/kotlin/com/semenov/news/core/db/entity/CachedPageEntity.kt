package com.semenov.news.core.db.entity

import androidx.room.Entity

/**
 * Marks that [page] of [contextKey] was downloaded from the API and stored in [ArticleEntity] rows.
 * A page can be cached with zero rows (empty API page or all articles already stored on earlier pages).
 */
@Entity(
    tableName = "cached_pages",
    primaryKeys = ["contextKey", "page"],
)
data class CachedPageEntity(
    val contextKey: String,
    val page: Int,
    val totalResults: Int,
    val cachedAt: Long,
)
