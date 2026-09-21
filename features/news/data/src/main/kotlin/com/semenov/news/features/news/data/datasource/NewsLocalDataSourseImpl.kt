package com.semenov.news.features.news.data.datasource

import androidx.room.withTransaction
import com.semenov.news.core.db.NewsDatabase
import com.semenov.news.core.db.dao.ArticleDao
import com.semenov.news.core.db.mapper.toDomain
import com.semenov.news.core.db.mapper.toEntityOrNull
import com.semenov.news.core.domain.model.Article
import com.semenov.news.core.domain.model.NewsRequest
import com.semenov.news.features.news.domain.datasource.NewsLocalDataSourse
import javax.inject.Inject

class NewsLocalDataSourseImpl @Inject constructor(
    private val database: NewsDatabase,
    private val articleDao: ArticleDao,
) : NewsLocalDataSourse {
    override suspend fun articles(request: NewsRequest): List<Article> =
        articleDao.articlesForContext(request.contextKey()).map { it.toDomain() }

    override suspend fun storePage(
        request: NewsRequest,
        page: Int,
        articles: List<Article>,
    ) {
        val contextKey = request.contextKey()
        val cachedAt = System.currentTimeMillis()
        val entities =
            articles.mapIndexedNotNull { position, article ->
                article.toEntityOrNull(
                    contextKey = contextKey,
                    page = page,
                    position = position,
                    cachedAt = cachedAt,
                )
            }

        database.withTransaction {
            if (page == FIRST_PAGE) {
                articleDao.deleteContext(contextKey)
            }
            if (entities.isNotEmpty()) {
                articleDao.upsertArticles(entities)
            }
        }
    }

    private companion object {
        const val FIRST_PAGE = 1
    }
}

internal fun NewsRequest.contextKey(): String =
    when (this) {
        NewsRequest.Feed -> "feed"
        is NewsRequest.Search -> "search:${query.trim().lowercase()}"
        is NewsRequest.Category -> "category:${category.trim().lowercase()}"
    }
