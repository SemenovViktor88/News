package com.semenov.news.features.news.domain.datasource

import com.semenov.news.core.domain.model.NewsPage
import com.semenov.news.core.domain.model.NewsRequest

interface NewsLocalDataSourse {
    /** Returns the stored page, or `null` if this page has never been downloaded for [request]. */
    suspend fun page(
        request: NewsRequest,
        page: Int,
    ): NewsPage?

    /** Page 1 replaces the whole context: later pages of an outdated snapshot are dropped. */
    suspend fun storePage(
        request: NewsRequest,
        page: Int,
        newsPage: NewsPage,
    )
}
