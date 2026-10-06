package com.semenov.news.features.news.data.repository

import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.domain.model.DataResult
import com.semenov.news.core.domain.model.NetworkError
import com.semenov.news.core.domain.model.NewsCategory
import com.semenov.news.core.domain.model.NewsPage
import com.semenov.news.core.domain.model.NewsRequest
import com.semenov.news.core.domain.model.NewsSource
import com.semenov.news.core.domain.model.SearchNewsRequest
import com.semenov.news.core.domain.model.SourcesRequest
import com.semenov.news.core.domain.model.TopHeadlinesRequest
import com.semenov.news.features.news.data.datasource.contextKey
import com.semenov.news.features.news.domain.datasource.NewsLocalDataSourse
import com.semenov.news.features.news.domain.datasource.NewsRemoteDataSourse
import com.semenov.news.features.news.domain.repository.NewsRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class NewsRepositoryImpl @Inject constructor(
    private val remoteDataSourse: NewsRemoteDataSourse,
    private val localDataSourse: NewsLocalDataSourse,
    private val logger: AppLogger,
) : NewsRepository {
    private val inFlightMutex = Mutex()
    private val inFlightPages = mutableMapOf<PageKey, CompletableDeferred<DataResult<NewsPage>>>()

    override suspend fun loadPage(
        request: NewsRequest,
        page: Int,
        forceRefresh: Boolean,
    ): DataResult<NewsPage> {
        require(page > 0) { "Page must be greater than zero" }
        if (!forceRefresh) {
            cachedPage(request, page)?.let { return DataResult.Success(it) }
        }
        return fetchPageOnce(request, page) {
            if (!forceRefresh) {
                cachedPage(request, page)?.let { return@fetchPageOnce DataResult.Success(it) }
            }
            fetchAndCache(request, page)
        }
    }

    override suspend fun prefetchPage(
        request: NewsRequest,
        page: Int,
    ): DataResult<Unit> {
        require(page > 0) { "Page must be greater than zero" }
        if (cachedPage(request, page) != null) return DataResult.Success(Unit)
        val result =
            fetchPageOnce(request, page) {
                cachedPage(request, page)?.let { return@fetchPageOnce DataResult.Success(it) }
                fetchAndCache(request, page)
            }
        return when (result) {
            is DataResult.Success -> DataResult.Success(Unit)
            is DataResult.Failure -> result
        }
    }

    override suspend fun cachedPage(
        request: NewsRequest,
        page: Int,
    ): NewsPage? =
        try {
            localDataSourse.page(request, page)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            logger.log(TAG, "Failed to read cached page $page for $request", throwable)
            null
        }

    override suspend fun getSources(request: SourcesRequest): DataResult<List<NewsSource>> =
        remoteDataSourse.getSources(request)

    private suspend fun fetchAndCache(
        request: NewsRequest,
        page: Int,
    ): DataResult<NewsPage> {
        val remoteResult =
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

        val result =
            if (remoteResult is DataResult.Failure && remoteResult.error.isUpgradeRequired()) {
                DataResult.Success(NewsPage(articles = emptyList(), totalResults = FREE_TIER_LIMIT))
            } else {
                remoteResult
            }
        return when (result) {
            is DataResult.Success -> storeAndReadPage(request, page, result.data)
            is DataResult.Failure -> result
        }
    }

    private suspend fun storeAndReadPage(
        request: NewsRequest,
        page: Int,
        newsPage: NewsPage,
    ): DataResult<NewsPage> =
        try {
            localDataSourse.storePage(request, page, newsPage)
            val storedPage = localDataSourse.page(request, page)
            if (storedPage == null) {
                DataResult.Failure(
                    NetworkError.Unknown("Page $page was not found after it was cached"),
                )
            } else {
                DataResult.Success(storedPage)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            logger.log(TAG, "Failed to cache page $page for $request", throwable)
            DataResult.Failure(
                NetworkError.Unknown(throwable.message ?: "Failed to cache page $page"),
            )
        }

    /** Shares one network-and-cache operation between every caller of the same page. */
    private suspend fun fetchPageOnce(
        request: NewsRequest,
        page: Int,
        fetch: suspend () -> DataResult<NewsPage>,
    ): DataResult<NewsPage> {
        val key = PageKey(request.contextKey(), page)
        val (deferred, isOwner) =
            inFlightMutex.withLock {
                val existing = inFlightPages[key]
                if (existing != null) {
                    existing to false
                } else {
                    CompletableDeferred<DataResult<NewsPage>>()
                        .also { inFlightPages[key] = it } to true
                }
            }
        if (!isOwner) return deferred.await()

        try {
            val result = fetch()
            deferred.complete(result)
            return result
        } catch (cancellation: CancellationException) {
            deferred.cancel(cancellation)
            throw cancellation
        } catch (throwable: Throwable) {
            deferred.completeExceptionally(throwable)
            throw throwable
        } finally {
            withContext(NonCancellable) {
                inFlightMutex.withLock {
                    if (inFlightPages[key] === deferred) inFlightPages.remove(key)
                }
            }
        }
    }

    private fun NetworkError.isUpgradeRequired(): Boolean =
        this is NetworkError.ClientError && statusCode == UPGRADE_REQUIRED_STATUS

    private data class PageKey(
        val contextKey: String,
        val page: Int,
    )

    private companion object {
        const val TAG = "NewsRepository"
        const val DEFAULT_HEADLINES_COUNTRY = "us"
        const val PAGE_SIZE = 20
        const val FREE_TIER_LIMIT = 100
        const val UPGRADE_REQUIRED_STATUS = 426
    }
}
