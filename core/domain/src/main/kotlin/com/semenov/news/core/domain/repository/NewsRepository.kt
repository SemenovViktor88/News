package com.semenov.news.core.domain.repository

import com.semenov.news.core.domain.model.DataResult
import com.semenov.news.core.domain.model.NewsPage
import com.semenov.news.core.domain.model.NewsSource
import com.semenov.news.core.domain.model.SearchNewsRequest
import com.semenov.news.core.domain.model.SourcesRequest
import com.semenov.news.core.domain.model.TopHeadlinesRequest

interface NewsRepository {
    suspend fun getTopHeadlines(request: TopHeadlinesRequest): DataResult<NewsPage>

    suspend fun searchNews(request: SearchNewsRequest): DataResult<NewsPage>

    suspend fun getSources(request: SourcesRequest = SourcesRequest()): DataResult<List<NewsSource>>
}
