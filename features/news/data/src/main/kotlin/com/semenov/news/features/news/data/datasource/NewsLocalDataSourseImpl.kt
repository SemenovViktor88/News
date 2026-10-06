package com.semenov.news.features.news.data.datasource

import androidx.room.withTransaction
import com.semenov.news.core.db.NewsDatabase
import com.semenov.news.core.db.dao.ArticleDao
import com.semenov.news.core.db.entity.CachedPageEntity
import com.semenov.news.core.db.mapper.toDomain
import com.semenov.news.core.db.mapper.toEntityOrNull
import com.semenov.news.core.domain.model.NewsPage
import com.semenov.news.core.domain.model.NewsRequest
import com.semenov.news.features.news.domain.datasource.NewsLocalDataSourse
import javax.inject.Inject

class NewsLocalDataSourseImpl @Inject constructor(
    private val database: NewsDatabase,
    private val articleDao: ArticleDao,
) : NewsLocalDataSourse {
    override suspend fun page(
        request: NewsRequest,
        page: Int,
    ): NewsPage? {
        val contextKey = request.contextKey()
        return database.withTransaction {
            deleteStaleContexts(System.currentTimeMillis())
            val cachedPage = articleDao.cachedPage(contextKey, page) ?: return@withTransaction null
            NewsPage(
                articles = articleDao.articlesForPage(contextKey, page).map { it.toDomain() },
                totalResults = cachedPage.totalResults,
            )
        }
    }

    override suspend fun storePage(
        request: NewsRequest,
        page: Int,
        newsPage: NewsPage,
    ) {
        val contextKey = request.contextKey()
        val cachedAt = System.currentTimeMillis()
        val entities =
            newsPage.articles.mapIndexedNotNull { position, article ->
                article.toEntityOrNull(
                    contextKey = contextKey,
                    page = page,
                    position = position,
                    cachedAt = cachedAt,
                )
            }

        database.withTransaction {
            deleteStaleContexts(cachedAt)
            if (page == FIRST_PAGE) {
                articleDao.deleteContext(contextKey)
                articleDao.deleteCachedPages(contextKey)
            }
            if (entities.isNotEmpty()) {
                articleDao.insertArticlesIgnoringDuplicates(entities)
            }
            articleDao.upsertCachedPage(
                CachedPageEntity(
                    contextKey = contextKey,
                    page = page,
                    totalResults = newsPage.totalResults,
                    cachedAt = cachedAt,
                ),
            )
        }
    }

    /** Removes whole snapshots so page markers can never outlive articles from the same context. */
    private suspend fun deleteStaleContexts(now: Long) {
        val staleBefore = now - CACHE_LIFETIME_MS
        articleDao.deleteArticlesFromStaleContexts(staleBefore)
        articleDao.deleteStaleContexts(staleBefore)
    }

    private companion object {
        const val FIRST_PAGE = 1

        /** Contexts nobody refreshed for a day (old search queries, unused categories) are dropped. */
        const val CACHE_LIFETIME_MS = 24 * 60 * 60 * 1000L
    }
}

internal fun NewsRequest.contextKey(): String =
    when (this) {
        NewsRequest.Feed -> "feed"
        is NewsRequest.Search -> "search:${query.trim().lowercase()}"
        is NewsRequest.Category -> "category:${category.trim().lowercase()}"
    }
