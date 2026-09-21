package com.semenov.news.features.news.data.repository

import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.domain.model.Article
import com.semenov.news.core.domain.model.DataResult
import com.semenov.news.core.domain.model.NetworkError
import com.semenov.news.core.domain.model.NewsCategory
import com.semenov.news.core.domain.model.NewsPage
import com.semenov.news.core.domain.model.NewsRequest
import com.semenov.news.core.domain.model.NewsSource
import com.semenov.news.core.domain.model.SearchNewsRequest
import com.semenov.news.core.domain.model.SourcesRequest
import com.semenov.news.core.domain.model.TopHeadlinesRequest
import com.semenov.news.features.news.domain.datasource.NewsLocalDataSourse
import com.semenov.news.features.news.domain.datasource.NewsRemoteDataSourse
import com.semenov.news.features.news.domain.repository.NewsRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class NewsRepositoryImpl @Inject constructor(
    private val remoteDataSourse: NewsRemoteDataSourse,
    private val localDataSourse: NewsLocalDataSourse,
    private val logger: AppLogger,
) : NewsRepository {
    override suspend fun loadPage(
        request: NewsRequest,
        page: Int,
    ): DataResult<NewsPage> {
        require(page > 0) { "Page must be greater than zero" }
        val result =
            when (request) {
                NewsRequest.Feed ->
                    remoteDataSourse.getTopHeadlines(
                        TopHeadlinesRequest(
                            country = DEFAULT_HEADLINES_COUNTRY,
                            page = page,
                            pageSize = PAGE_SIZE,
                        ),
                    )

                is NewsRequest.Search ->
                    remoteDataSourse.searchNews(
                        SearchNewsRequest(
                            query = request.query.trim(),
                            page = page,
                            pageSize = PAGE_SIZE,
                        ),
                    )

                is NewsRequest.Category -> {
                    val category =
                        NewsCategory.entries.firstOrNull {
                            it.apiValue.equals(request.category.trim(), ignoreCase = true)
                        } ?: return DataResult.Failure(
                            NetworkError.BadRequest("Unknown category: ${request.category}"),
                        )
                    remoteDataSourse.getTopHeadlines(
                        TopHeadlinesRequest(
                            country = DEFAULT_HEADLINES_COUNTRY,
                            category = category,
                            page = page,
                            pageSize = PAGE_SIZE,
                        ),
                    )
                }
            }

        if (result is DataResult.Failure && result.error.isUpgradeRequired()) {
            return DataResult.Success(NewsPage(articles = emptyList(), totalResults = FREE_TIER_LIMIT))
        }
        if (result is DataResult.Success) {
            cachePage(request, page, result.data.articles)
        }
        return result
    }

    override suspend fun cachedArticles(request: NewsRequest): List<Article> =
        try {
            localDataSourse.articles(request)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            logger.log(TAG, "Failed to read cached articles for $request", throwable)
            emptyList()
        }

    override suspend fun getSources(request: SourcesRequest): DataResult<List<NewsSource>> =
        remoteDataSourse.getSources(request)

    private suspend fun cachePage(
        request: NewsRequest,
        page: Int,
        articles: List<Article>,
    ) {
        try {
            localDataSourse.storePage(request, page, articles)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            logger.log(TAG, "Failed to cache page $page for $request", throwable)
        }
    }

    private fun NetworkError.isUpgradeRequired(): Boolean =
        this is NetworkError.ClientError && statusCode == UPGRADE_REQUIRED_STATUS

    private companion object {
        const val TAG = "NewsRepository"
        const val DEFAULT_HEADLINES_COUNTRY = "us"
        const val PAGE_SIZE = 20
        const val FREE_TIER_LIMIT = 100
        const val UPGRADE_REQUIRED_STATUS = 426
    }
}
