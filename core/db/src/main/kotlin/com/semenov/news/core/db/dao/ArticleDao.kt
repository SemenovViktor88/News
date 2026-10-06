package com.semenov.news.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.semenov.news.core.db.entity.ArticleEntity
import com.semenov.news.core.db.entity.CachedPageEntity

@Dao
interface ArticleDao {
    /**
     * Keeps the first stored copy of an article: when NewsAPI shifts results, an article
     * already cached on an earlier page must not be moved to a later one.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticlesIgnoringDuplicates(entities: List<ArticleEntity>)

    @Query(
        """
        SELECT * FROM articles
        WHERE contextKey = :contextKey AND page = :page
        ORDER BY position ASC
        """,
    )
    suspend fun articlesForPage(
        contextKey: String,
        page: Int,
    ): List<ArticleEntity>

    @Upsert
    suspend fun upsertCachedPage(page: CachedPageEntity)

    @Query("SELECT * FROM cached_pages WHERE contextKey = :contextKey AND page = :page")
    suspend fun cachedPage(
        contextKey: String,
        page: Int,
    ): CachedPageEntity?

    @Query("DELETE FROM articles WHERE contextKey = :contextKey")
    suspend fun deleteContext(contextKey: String)

    @Query("DELETE FROM cached_pages WHERE contextKey = :contextKey")
    suspend fun deleteCachedPages(contextKey: String)

    @Query(
        """
        DELETE FROM articles
        WHERE contextKey IN (
            SELECT contextKey
            FROM cached_pages
            GROUP BY contextKey
            HAVING MAX(cachedAt) < :threshold
        )
        """,
    )
    suspend fun deleteArticlesFromStaleContexts(threshold: Long)

    @Query(
        """
        DELETE FROM cached_pages
        WHERE contextKey IN (
            SELECT contextKey
            FROM cached_pages
            GROUP BY contextKey
            HAVING MAX(cachedAt) < :threshold
        )
        """,
    )
    suspend fun deleteStaleContexts(threshold: Long)
}
