package com.semenov.news.features.news.domain.datasource

import com.semenov.news.core.domain.model.DataResult
import com.semenov.news.core.domain.model.NewsPage
import com.semenov.news.core.domain.model.NewsSource
import com.semenov.news.core.domain.model.SearchNewsRequest
import com.semenov.news.core.domain.model.SourcesRequest
import com.semenov.news.core.domain.model.TopHeadlinesRequest

interface NewsRemoteDataSourse {
    suspend fun getTopHeadlines(request: TopHeadlinesRequest): DataResult<NewsPage>

    suspend fun searchNews(request: SearchNewsRequest): DataResult<NewsPage>

    suspend fun getSources(request: SourcesRequest): DataResult<List<NewsSource>>
}
