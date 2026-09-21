package com.semenov.news.core.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.semenov.news.core.db.entity.ArticleEntity

@Dao
interface ArticleDao {
    @Upsert
    suspend fun upsertArticles(entities: List<ArticleEntity>)

    @Query(
        """
        SELECT * FROM articles
        WHERE contextKey = :contextKey
        ORDER BY page ASC, position ASC
        """,
    )
    suspend fun articlesForContext(contextKey: String): List<ArticleEntity>

    @Query("DELETE FROM articles WHERE contextKey = :contextKey")
    suspend fun deleteContext(contextKey: String)
}
