package com.semenov.news.features.news.domain.datasource

import com.semenov.news.core.domain.model.Article
import com.semenov.news.core.domain.model.NewsRequest

interface NewsLocalDataSourse {
    suspend fun articles(request: NewsRequest): List<Article>

    suspend fun storePage(
        request: NewsRequest,
        page: Int,
        articles: List<Article>,
    )
}
