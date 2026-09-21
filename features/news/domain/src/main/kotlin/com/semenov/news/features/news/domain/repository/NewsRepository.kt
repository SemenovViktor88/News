package com.semenov.news.features.news.domain.repository

import com.semenov.news.core.domain.model.Article
import com.semenov.news.core.domain.model.DataResult
import com.semenov.news.core.domain.model.NewsPage
import com.semenov.news.core.domain.model.NewsRequest
import com.semenov.news.core.domain.model.NewsSource
import com.semenov.news.core.domain.model.SourcesRequest

interface NewsRepository {
    suspend fun loadPage(
        request: NewsRequest,
        page: Int,
    ): DataResult<NewsPage>

    suspend fun cachedArticles(request: NewsRequest): List<Article>

    suspend fun getSources(request: SourcesRequest = SourcesRequest()): DataResult<List<NewsSource>>
}
