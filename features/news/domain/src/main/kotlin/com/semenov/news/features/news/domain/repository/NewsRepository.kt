package com.semenov.news.features.news.domain.repository

import com.semenov.news.core.domain.model.DataResult
import com.semenov.news.core.domain.model.NewsPage
import com.semenov.news.core.domain.model.NewsRequest
import com.semenov.news.core.domain.model.NewsSource
import com.semenov.news.core.domain.model.SourcesRequest

interface NewsRepository {
    /**
     * Returns [page] from the database. The API is called only when the page is not cached yet
     * or [forceRefresh] is set; the response is stored first and then read back from the database.
     */
    suspend fun loadPage(
        request: NewsRequest,
        page: Int,
        forceRefresh: Boolean = false,
    ): DataResult<NewsPage>

    /** Downloads [page] into the database ahead of time; does nothing if it is already cached. */
    suspend fun prefetchPage(
        request: NewsRequest,
        page: Int,
    ): DataResult<Unit>

    /** Database-only read, never touches the network. */
    suspend fun cachedPage(
        request: NewsRequest,
        page: Int,
    ): NewsPage?

    suspend fun getSources(request: SourcesRequest = SourcesRequest()): DataResult<List<NewsSource>>
}
