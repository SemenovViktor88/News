package com.semenov.news.features.news.data.repository

import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.domain.model.Article
import com.semenov.news.core.domain.model.DataResult
import com.semenov.news.core.domain.model.NetworkError
import com.semenov.news.core.domain.model.NewsPage
import com.semenov.news.core.domain.model.NewsRequest
import com.semenov.news.core.domain.model.NewsSource
import com.semenov.news.core.domain.model.SearchNewsRequest
import com.semenov.news.core.domain.model.SourcesRequest
import com.semenov.news.core.domain.model.TopHeadlinesRequest
import com.semenov.news.features.news.data.datasource.contextKey
import com.semenov.news.features.news.domain.datasource.NewsLocalDataSourse
import com.semenov.news.features.news.domain.datasource.NewsRemoteDataSourse
import java.util.ArrayDeque
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsRepositoryImplTest {
    @Test
    fun `forced page one refresh clears and rewrites only its context`() = runTest {
        val local = FakeLocalDataSource()
        val request = NewsRequest.Search(" Android ")
        local.seed(request, page = 1, listOf(article("Old")))
        local.seed(request, page = 2, listOf(article("Old page 2")))
        val repository = repository(FakeRemoteDataSource(success(article("New"))), local)

        val result = repository.loadPage(request, page = 1, forceRefresh = true)

        assertEquals(DataResult.Success(NewsPage(listOf(article("New")), 40)), result)
        assertEquals(listOf(article("New")), repository.cachedPage(request, 1)?.articles)
        assertNull(repository.cachedPage(request, 2))
        assertEquals(listOf(StoreCall("search:android", 1)), local.storeCalls)
    }

    @Test
    fun `cached page is served from the database without calling the api`() = runTest {
        val local = FakeLocalDataSource()
        val remote = FakeRemoteDataSource()
        val request = NewsRequest.Feed
        local.seed(request, page = 2, listOf(article("Cached page 2")))
        val repository = repository(remote, local)

        val result = repository.loadPage(request, page = 2)

        assertEquals(DataResult.Success(NewsPage(listOf(article("Cached page 2")), 40)), result)
        assertEquals(0, remote.calls)
    }

    @Test
    fun `missing page is downloaded, stored and returned from the database`() = runTest {
        val local = FakeLocalDataSource()
        val remote = FakeRemoteDataSource(success(article("Page 2")))
        val request = NewsRequest.Feed
        local.seed(request, page = 1, listOf(article("Page 1")))
        val repository = repository(remote, local)

        val first = repository.loadPage(request, page = 2)
        val second = repository.loadPage(request, page = 2)

        assertEquals(DataResult.Success(NewsPage(listOf(article("Page 2")), 40)), first)
        assertEquals(first, second)
        assertEquals(1, remote.calls)
        assertEquals(listOf(StoreCall("feed", 2)), local.storeCalls)
    }

    @Test
    fun `prefetch stores the next page so load more does not hit the network`() = runTest {
        val local = FakeLocalDataSource()
        val remote = FakeRemoteDataSource(success(article("Page 3")))
        val request = NewsRequest.Category("technology")
        val repository = repository(remote, local)

        val prefetch = repository.prefetchPage(request, page = 3)
        val loaded = repository.loadPage(request, page = 3)

        assertEquals(DataResult.Success(Unit), prefetch)
        assertEquals(DataResult.Success(NewsPage(listOf(article("Page 3")), 40)), loaded)
        assertEquals(1, remote.calls)
    }

    @Test
    fun `prefetch of an already cached page does nothing`() = runTest {
        val local = FakeLocalDataSource()
        val remote = FakeRemoteDataSource()
        val request = NewsRequest.Feed
        local.seed(request, page = 2, listOf(article("Cached")))
        val repository = repository(remote, local)

        assertEquals(DataResult.Success(Unit), repository.prefetchPage(request, page = 2))
        assertEquals(0, remote.calls)
        assertTrue(local.storeCalls.isEmpty())
    }

    @Test
    fun `failed prefetch leaves nothing cached and reports the error`() = runTest {
        val local = FakeLocalDataSource()
        val repository = repository(FakeRemoteDataSource(failure()), local)

        assertEquals(failure(), repository.prefetchPage(NewsRequest.Feed, page = 2))
        assertNull(repository.cachedPage(NewsRequest.Feed, 2))
    }

    @Test
    fun `cache write failure is reported instead of returning network data`() = runTest {
        val local = FakeLocalDataSource(storeFailure = IllegalStateException("Disk is full"))
        val remote = FakeRemoteDataSource(success(article("Network only")))
        val repository = repository(remote, local)

        val result = repository.loadPage(NewsRequest.Feed, page = 2)

        assertEquals(
            DataResult.Failure(NetworkError.Unknown("Disk is full")),
            result,
        )
        assertNull(repository.cachedPage(NewsRequest.Feed, 2))
        assertEquals(1, remote.calls)
    }

    @Test
    fun `concurrent prefetch and load share one network request`() = runTest {
        val requestStarted = CompletableDeferred<Unit>()
        val releaseResponse = CompletableDeferred<Unit>()
        val remote =
            BlockingRemoteDataSource(
                requestStarted = requestStarted,
                releaseResponse = releaseResponse,
                response = success(article("Shared")),
            )
        val repository = repository(remote, FakeLocalDataSource())

        val prefetch = async { repository.prefetchPage(NewsRequest.Feed, page = 2) }
        requestStarted.await()
        val load = async { repository.loadPage(NewsRequest.Feed, page = 2) }
        yield()

        assertEquals(1, remote.calls)
        releaseResponse.complete(Unit)
        assertEquals(DataResult.Success(Unit), prefetch.await())
        assertEquals(
            DataResult.Success(NewsPage(listOf(article("Shared")), 40)),
            load.await(),
        )
        assertEquals(1, remote.calls)
    }

    @Test
    fun `remote failure keeps cached articles untouched`() = runTest {
        val local = FakeLocalDataSource()
        val request = NewsRequest.Category("technology")
        val cached = listOf(article("Cached"))
        local.seed(request, page = 1, cached)
        val repository = repository(FakeRemoteDataSource(failure()), local)

        val result = repository.loadPage(request, page = 1, forceRefresh = true)

        assertEquals(failure(), result)
        assertEquals(cached, repository.cachedPage(request, 1)?.articles)
        assertTrue(local.storeCalls.isEmpty())
    }

    @Test
    fun `remote failure without cache returns error`() = runTest {
        val local = FakeLocalDataSource()
        val repository = repository(FakeRemoteDataSource(failure()), local)

        val result = repository.loadPage(NewsRequest.Feed, page = 1)

        assertEquals(failure(), result)
        assertNull(repository.cachedPage(NewsRequest.Feed, 1))
    }

    @Test
    fun `different cache contexts never overwrite each other`() = runTest {
        val local = FakeLocalDataSource()
        val remote =
            FakeRemoteDataSource(
                success(article("Feed")),
                success(article("Sports")),
            )
        val repository = repository(remote, local)

        repository.loadPage(NewsRequest.Feed, page = 1, forceRefresh = true)
        repository.loadPage(NewsRequest.Category("SPORTS"), page = 1, forceRefresh = true)

        assertEquals(listOf(article("Feed")), repository.cachedPage(NewsRequest.Feed, 1)?.articles)
        assertEquals(
            listOf(article("Sports")),
            repository.cachedPage(NewsRequest.Category("sports"), 1)?.articles,
        )
    }

    @Test
    fun `upgrade required response becomes end of pagination`() = runTest {
        val local = FakeLocalDataSource()
        val remote =
            FakeRemoteDataSource(
                DataResult.Failure(NetworkError.ClientError(statusCode = 426, message = "Upgrade")),
            )
        val repository = repository(remote, local)

        val result = repository.loadPage(NewsRequest.Feed, page = 6)

        assertEquals(
            DataResult.Success(NewsPage(articles = emptyList(), totalResults = 100)),
            result,
        )
        assertEquals(listOf(StoreCall("feed", 6)), local.storeCalls)
        assertEquals(
            NewsPage(articles = emptyList(), totalResults = 100),
            repository.cachedPage(NewsRequest.Feed, 6),
        )
    }

    private fun repository(
        remote: NewsRemoteDataSourse,
        local: NewsLocalDataSourse,
    ) = NewsRepositoryImpl(remote, local, NoOpLogger)

    /** Mirrors NewsLocalDataSourseImpl: pages are keyed by context, page 1 replaces the context. */
    private class FakeLocalDataSource(
        private val storeFailure: Throwable? = null,
    ) : NewsLocalDataSourse {
        private val pages = mutableMapOf<String, MutableMap<Int, NewsPage>>()
        val storeCalls = mutableListOf<StoreCall>()

        override suspend fun page(
            request: NewsRequest,
            page: Int,
        ): NewsPage? = pages[request.contextKey()]?.get(page)

        override suspend fun storePage(
            request: NewsRequest,
            page: Int,
            newsPage: NewsPage,
        ) {
            val key = request.contextKey()
            storeCalls += StoreCall(key, page)
            storeFailure?.let { throw it }
            if (page == 1) pages[key] = mutableMapOf()
            pages.getOrPut(key, ::mutableMapOf)[page] = newsPage
        }

        fun seed(
            request: NewsRequest,
            page: Int,
            articles: List<Article>,
        ) {
            pages.getOrPut(request.contextKey(), ::mutableMapOf)[page] = NewsPage(articles, 40)
        }
    }

    private class FakeRemoteDataSource(
        vararg responses: DataResult<NewsPage>,
    ) : NewsRemoteDataSourse {
        private val responses = ArrayDeque(responses.toList())
        var calls = 0
            private set

        override suspend fun getTopHeadlines(request: TopHeadlinesRequest): DataResult<NewsPage> {
            calls++
            return responses.removeFirst()
        }

        override suspend fun searchNews(request: SearchNewsRequest): DataResult<NewsPage> {
            calls++
            return responses.removeFirst()
        }

        override suspend fun getSources(request: SourcesRequest): DataResult<List<NewsSource>> =
            error("Not expected")
    }

    private class BlockingRemoteDataSource(
        private val requestStarted: CompletableDeferred<Unit>,
        private val releaseResponse: CompletableDeferred<Unit>,
        private val response: DataResult<NewsPage>,
    ) : NewsRemoteDataSourse {
        var calls = 0
            private set

        override suspend fun getTopHeadlines(request: TopHeadlinesRequest): DataResult<NewsPage> {
            calls++
            requestStarted.complete(Unit)
            releaseResponse.await()
            return response
        }

        override suspend fun searchNews(request: SearchNewsRequest): DataResult<NewsPage> =
            error("Not expected")

        override suspend fun getSources(request: SourcesRequest): DataResult<List<NewsSource>> =
            error("Not expected")
    }

    private object NoOpLogger : AppLogger {
        override fun log(
            tag: String,
            message: String,
            t: Throwable?,
        ) = Unit
    }

    private data class StoreCall(val contextKey: String, val page: Int)

    private companion object {
        fun article(title: String) =
            Article(
                source = "Source",
                author = null,
                title = title,
                description = null,
                url = "https://example.com/$title",
                imageUrl = null,
                publishedAt = null,
                content = null,
            )

        fun success(vararg articles: Article): DataResult<NewsPage> =
            DataResult.Success(NewsPage(articles.toList(), 40))

        fun failure(): DataResult<NewsPage> = DataResult.Failure(NetworkError.NoInternet)
    }
}
