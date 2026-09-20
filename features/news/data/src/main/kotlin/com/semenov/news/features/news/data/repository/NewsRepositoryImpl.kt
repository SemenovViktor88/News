package com.semenov.news.features.news.data.repository

import com.semenov.news.core.domain.model.DataResult
import com.semenov.news.core.domain.model.NewsPage
import com.semenov.news.core.domain.model.NewsSource
import com.semenov.news.core.domain.model.SearchNewsRequest
import com.semenov.news.core.domain.model.SourcesRequest
import com.semenov.news.core.domain.model.TopHeadlinesRequest
import com.semenov.news.core.network.data.datasource.NewsApiDataSource
import com.semenov.news.features.news.domain.repository.NewsRepository
import javax.inject.Inject

class NewsRepositoryImpl @Inject constructor(
    private val remoteDataSource: NewsApiDataSource,
) : NewsRepository {
    override suspend fun getTopHeadlines(request: TopHeadlinesRequest): DataResult<NewsPage> =
        remoteDataSource.getTopHeadlines(request)

    override suspend fun searchNews(request: SearchNewsRequest): DataResult<NewsPage> =
        remoteDataSource.searchNews(request)

    override suspend fun getSources(request: SourcesRequest): DataResult<List<NewsSource>> =
        remoteDataSource.getSources(request)
}
