package com.semenov.news.screens.home

import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.domain.model.Article
import com.semenov.news.core.domain.model.DataResult
import com.semenov.news.core.domain.model.NetworkError
import com.semenov.news.core.domain.model.NewsPage
import com.semenov.news.core.domain.model.NewsRequest
import com.semenov.news.core.domain.model.NewsSource
import com.semenov.news.core.domain.model.SourcesRequest
import com.semenov.news.features.news.domain.repository.NewsRepository
import com.semenov.news.screens.home.model.HomeIntent
import com.semenov.news.screens.home.reducer.HomeReducer
import java.util.ArrayDeque
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load renders cache before replacing it with network page`() = runTest(dispatcher) {
        val networkResult = CompletableDeferred<DataResult<NewsPage>>()
        val repository =
            FakeNewsRepository(
                cached = mapOf(NewsRequest.Feed to listOf(article("Cached", 0))),
                response = { _, _ -> networkResult.await() },
            )
        val viewModel = createViewModel(repository)
        val state = viewModel.state

        runCurrent()

        assertEquals(listOf("Cached"), state.value.articles.map(Article::title))
        assertTrue(state.value.hasLoaded)
        assertTrue(state.value.isSearching)
        assertFalse(state.value.isLoading)

        networkResult.complete(successPage("Fresh", page = 1))
        advanceUntilIdle()

        assertEquals(20, state.value.articles.size)
        assertEquals("Fresh-1", state.value.articles.first().title)
        assertFalse(state.value.isSearching)
        assertEquals(1, state.value.currentPage)
    }

    @Test
    fun `load more appends the next page`() = runTest(dispatcher) {
        val repository =
            FakeNewsRepository { _, page -> successPage("Page", page) }
        val viewModel = createViewModel(repository)
        val state = viewModel.state
        advanceUntilIdle()

        viewModel.processIntent(HomeIntent.LoadMore)
        advanceUntilIdle()

        assertEquals(40, state.value.articles.size)
        assertEquals("Page-21", state.value.articles[20].title)
        assertEquals(2, state.value.currentPage)
        assertTrue(state.value.hasMore)
    }

    @Test
    fun `short first page still loads next page when total results are larger`() = runTest(dispatcher) {
        val repository =
            FakeNewsRepository { _, page ->
                successPage(
                    prefix = "Page",
                    page = page,
                    totalResults = 33,
                    articleCount = if (page == 1) 19 else 13,
                )
            }
        val viewModel = createViewModel(repository)
        val state = viewModel.state
        advanceUntilIdle()

        assertEquals(19, state.value.articles.size)
        assertTrue(state.value.hasMore)

        viewModel.processIntent(HomeIntent.LoadMore)
        advanceUntilIdle()

        assertEquals(listOf(1, 2), repository.calls.map(LoadCall::page))
        assertEquals(32, state.value.articles.size)
        assertEquals(2, state.value.currentPage)
        assertFalse(state.value.hasMore)
    }

    @Test
    fun `load more error keeps content and exposes footer error`() = runTest(dispatcher) {
        val repository =
            FakeNewsRepository { _, page ->
                if (page == 1) successPage("Page", page) else failure()
            }
        val viewModel = createViewModel(repository)
        val state = viewModel.state
        advanceUntilIdle()

        viewModel.processIntent(HomeIntent.LoadMore)
        advanceUntilIdle()

        assertEquals(20, state.value.articles.size)
        assertEquals(NetworkError.Timeout, state.value.loadMoreError)
        assertEquals(1, state.value.currentPage)
        assertFalse(state.value.isLoadingMore)
    }

    @Test
    fun `retry load more requests only the failed page`() = runTest(dispatcher) {
        val responses =
            ArrayDeque(
                listOf(
                    completed(successPage("Page", 1)),
                    completed(failure()),
                    completed(successPage("Page", 2)),
                ),
            )
        val repository = FakeNewsRepository(response = { _, _ -> responses.removeFirst().await() })
        val viewModel = createViewModel(repository)
        val state = viewModel.state
        advanceUntilIdle()

        viewModel.processIntent(HomeIntent.LoadMore)
        advanceUntilIdle()
        viewModel.processIntent(HomeIntent.RetryLoadMore)
        advanceUntilIdle()

        assertEquals(listOf(1, 2, 2), repository.calls.map(LoadCall::page))
        assertEquals(40, state.value.articles.size)
        assertNull(state.value.loadMoreError)
        assertEquals(2, state.value.currentPage)
    }

    @Test
    fun `new search resets pagination and never mixes feed articles`() = runTest(dispatcher) {
        val searchResult = CompletableDeferred<DataResult<NewsPage>>()
        val repository =
            FakeNewsRepository { request, page ->
                when (request) {
                    NewsRequest.Feed -> successPage("Feed", page)
                    is NewsRequest.Search -> searchResult.await()
                    is NewsRequest.Category -> error("Not expected")
                }
            }
        val viewModel = createViewModel(repository)
        val state = viewModel.state
        advanceUntilIdle()

        viewModel.processIntent(HomeIntent.SearchQueryChanged(" android "))
        runCurrent()
        advanceTimeBy(300.milliseconds)
        runCurrent()

        assertTrue(state.value.articles.isEmpty())
        assertTrue(state.value.isLoading)
        assertEquals(
            LoadCall(NewsRequest.Search("android"), 1),
            repository.calls.last(),
        )

        searchResult.complete(successPage("Android", page = 1))
        advanceUntilIdle()

        assertEquals(20, state.value.articles.size)
        assertTrue(state.value.articles.all { it.title?.startsWith("Android") == true })
        assertEquals(1, state.value.currentPage)
    }

    @Test
    fun `concurrent load more intents create one request`() = runTest(dispatcher) {
        val nextPage = CompletableDeferred<DataResult<NewsPage>>()
        val repository =
            FakeNewsRepository { _, page ->
                if (page == 1) successPage("Page", page) else nextPage.await()
            }
        val viewModel = createViewModel(repository)
        val state = viewModel.state
        advanceUntilIdle()

        repeat(3) { viewModel.processIntent(HomeIntent.LoadMore) }
        runCurrent()

        assertEquals(1, repository.calls.count { it.page == 2 })
        assertTrue(state.value.isLoadingMore)

        nextPage.complete(successPage("Page", page = 2))
        advanceUntilIdle()
        assertEquals(40, state.value.articles.size)
    }

    @Test
    fun `load more becomes available after cached page refresh completes`() = runTest(dispatcher) {
        val firstPage = CompletableDeferred<DataResult<NewsPage>>()
        val repository =
            FakeNewsRepository(
                cached = mapOf(NewsRequest.Feed to successArticles("Cached", page = 1)),
                response = { _, page ->
                    if (page == 1) firstPage.await() else successPage("Fresh", page)
                },
            )
        val viewModel = createViewModel(repository)
        val state = viewModel.state
        runCurrent()

        assertTrue(state.value.isSearching)
        viewModel.processIntent(HomeIntent.LoadMore)
        runCurrent()
        assertEquals(listOf(1), repository.calls.map(LoadCall::page))

        firstPage.complete(successPage("Fresh", page = 1))
        advanceUntilIdle()

        assertEquals(listOf(1, 2), repository.calls.map(LoadCall::page))
        assertEquals(40, state.value.articles.size)
    }

    @Test
    fun `pagination stops at NewsAPI free tier limit`() = runTest(dispatcher) {
        val repository =
            FakeNewsRepository { _, page -> successPage("Capped", page, totalResults = 200) }
        val viewModel = createViewModel(repository)
        val state = viewModel.state
        advanceUntilIdle()

        repeat(4) {
            viewModel.processIntent(HomeIntent.LoadMore)
            advanceUntilIdle()
        }

        assertEquals(100, state.value.articles.size)
        assertEquals(5, state.value.currentPage)
        assertFalse(state.value.hasMore)

        viewModel.processIntent(HomeIntent.LoadMore)
        advanceUntilIdle()
        assertEquals(listOf(1, 2, 3, 4, 5), repository.calls.map(LoadCall::page))
    }

    private fun createViewModel(repository: NewsRepository) =
        HomeViewModel(
            repository = repository,
            homeReducer = HomeReducer(),
            logger = NoOpLogger,
        )

    private class FakeNewsRepository(
        private val cached: Map<NewsRequest, List<Article>> = emptyMap(),
        private val response: suspend (NewsRequest, Int) -> DataResult<NewsPage>,
    ) : NewsRepository {
        val calls = mutableListOf<LoadCall>()

        override suspend fun loadPage(
            request: NewsRequest,
            page: Int,
        ): DataResult<NewsPage> {
            calls += LoadCall(request, page)
            return response(request, page)
        }

        override suspend fun cachedArticles(request: NewsRequest): List<Article> =
            cached[request].orEmpty()

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

    private data class LoadCall(val request: NewsRequest, val page: Int)

    private companion object {
        fun article(
            prefix: String,
            index: Int,
        ) = Article(
            source = "Source",
            author = null,
            title = if (index == 0) prefix else "$prefix-$index",
            description = null,
            url = "https://example.com/$prefix/$index",
            imageUrl = null,
            publishedAt = null,
            content = null,
        )

        fun successPage(
            prefix: String,
            page: Int,
            totalResults: Int = 60,
            articleCount: Int = 20,
        ): DataResult<NewsPage> {
            return DataResult.Success(
                NewsPage(
                    articles = successArticles(prefix, page, articleCount),
                    totalResults = totalResults,
                ),
            )
        }

        fun successArticles(
            prefix: String,
            page: Int,
            articleCount: Int = 20,
        ): List<Article> {
            val start = (page - 1) * 20 + 1
            return (start until start + articleCount).map { article(prefix, it) }
        }

        fun failure(): DataResult<NewsPage> = DataResult.Failure(NetworkError.Timeout)

        fun completed(result: DataResult<NewsPage>) =
            CompletableDeferred<DataResult<NewsPage>>().apply { complete(result) }
    }
}
