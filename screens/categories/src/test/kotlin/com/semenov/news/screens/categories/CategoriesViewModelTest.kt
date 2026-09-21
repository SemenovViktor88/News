package com.semenov.news.screens.categories

import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.domain.model.Article
import com.semenov.news.core.domain.model.DataResult
import com.semenov.news.core.domain.model.NetworkError
import com.semenov.news.core.domain.model.NewsCategory
import com.semenov.news.core.domain.model.NewsPage
import com.semenov.news.core.domain.model.NewsRequest
import com.semenov.news.core.domain.model.NewsSource
import com.semenov.news.core.domain.model.SourcesRequest
import com.semenov.news.features.news.domain.repository.NewsRepository
import com.semenov.news.screens.categories.model.CategoriesIntent
import com.semenov.news.screens.categories.reducer.CategoriesReducer
import java.util.ArrayDeque
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesViewModelTest {
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
    fun `initial category load requests general page one`() = runTest(dispatcher) {
        val repository = FakeNewsRepository { _, page -> successPage("General", page) }
        val viewModel = createViewModel(repository)
        val state = viewModel.state
        advanceUntilIdle()

        assertEquals(
            LoadCall(NewsRequest.Category("general"), 1),
            repository.calls.single(),
        )
        assertEquals(NewsCategory.GENERAL, state.value.selectedCategory)
        assertEquals(20, state.value.articles.size)
        assertEquals(1, state.value.currentPage)
    }

    @Test
    fun `category change resets pagination and cannot mix old results`() = runTest(dispatcher) {
        val sportsResult = CompletableDeferred<DataResult<NewsPage>>()
        val repository =
            FakeNewsRepository { request, page ->
                when (request) {
                    NewsRequest.Category("general") -> successPage("General", page)
                    NewsRequest.Category("sports") -> sportsResult.await()
                    else -> error("Not expected: $request")
                }
            }
        val viewModel = createViewModel(repository)
        val state = viewModel.state
        advanceUntilIdle()

        viewModel.processIntent(CategoriesIntent.CategorySelected(NewsCategory.SPORTS))
        runCurrent()

        assertEquals(NewsCategory.SPORTS, state.value.selectedCategory)
        assertTrue(state.value.articles.isEmpty())
        assertTrue(state.value.isLoading)
        assertEquals(1, state.value.currentPage)

        sportsResult.complete(successPage("Sports", page = 1))
        advanceUntilIdle()

        assertTrue(state.value.articles.all { it.title?.startsWith("Sports") == true })
        assertTrue(state.value.articles.none { it.title?.startsWith("General") == true })
    }

    @Test
    fun `load more error keeps list and retry appends failed page`() = runTest(dispatcher) {
        val responses =
            ArrayDeque(
                listOf(
                    completed(successPage("General", 1)),
                    completed(failure()),
                    completed(successPage("General", 2)),
                ),
            )
        val repository = FakeNewsRepository(response = { _, _ -> responses.removeFirst().await() })
        val viewModel = createViewModel(repository)
        val state = viewModel.state
        advanceUntilIdle()

        viewModel.processIntent(CategoriesIntent.LoadMore)
        advanceUntilIdle()

        assertEquals(20, state.value.articles.size)
        assertEquals(NetworkError.NoInternet, state.value.loadMoreError)
        assertEquals(1, state.value.currentPage)

        viewModel.processIntent(CategoriesIntent.RetryLoadMore)
        advanceUntilIdle()

        assertEquals(listOf(1, 2, 2), repository.calls.map(LoadCall::page))
        assertEquals(40, state.value.articles.size)
        assertNull(state.value.loadMoreError)
        assertEquals(2, state.value.currentPage)
    }

    @Test
    fun `short category page still loads next page when total results are larger`() =
        runTest(dispatcher) {
            val repository =
                FakeNewsRepository { _, page ->
                    successPage(
                        prefix = "General",
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

            viewModel.processIntent(CategoriesIntent.LoadMore)
            advanceUntilIdle()

            assertEquals(listOf(1, 2), repository.calls.map(LoadCall::page))
            assertEquals(32, state.value.articles.size)
            assertEquals(2, state.value.currentPage)
            assertFalse(state.value.hasMore)
        }

    @Test
    fun `cached category remains visible when refresh fails`() = runTest(dispatcher) {
        val request = NewsRequest.Category("general")
        val cachedArticle = article("Cached", 0)
        val repository =
            FakeNewsRepository(
                cached = mapOf(request to listOf(cachedArticle)),
                response = { _, _ -> failure() },
            )
        val viewModel = createViewModel(repository)
        val state = viewModel.state
        advanceUntilIdle()

        assertEquals(listOf(cachedArticle), state.value.articles)
        assertTrue(state.value.hasLoaded)
        assertFalse(state.value.isLoading)
        assertFalse(state.value.isSwitchingCategory)
        assertNull(state.value.error)
    }

    @Test
    fun `concurrent load more intents request one next page`() = runTest(dispatcher) {
        val nextPage = CompletableDeferred<DataResult<NewsPage>>()
        val repository =
            FakeNewsRepository { _, page ->
                if (page == 1) successPage("General", page) else nextPage.await()
            }
        val viewModel = createViewModel(repository)
        val state = viewModel.state
        advanceUntilIdle()

        repeat(3) { viewModel.processIntent(CategoriesIntent.LoadMore) }
        runCurrent()

        assertEquals(1, repository.calls.count { it.page == 2 })
        assertTrue(state.value.isLoadingMore)

        nextPage.complete(successPage("General", page = 2))
        advanceUntilIdle()
        assertEquals(40, state.value.articles.size)
    }

    @Test
    fun `load more becomes available after cached category refresh completes`() =
        runTest(dispatcher) {
            val request = NewsRequest.Category("general")
            val firstPage = CompletableDeferred<DataResult<NewsPage>>()
            val repository =
                FakeNewsRepository(
                    cached = mapOf(request to successArticles("Cached", page = 1)),
                    response = { _, page ->
                        if (page == 1) firstPage.await() else successPage("General", page)
                    },
                )
            val viewModel = createViewModel(repository)
            val state = viewModel.state
            runCurrent()

            assertTrue(state.value.isSwitchingCategory)
            viewModel.processIntent(CategoriesIntent.LoadMore)
            runCurrent()
            assertEquals(listOf(1), repository.calls.map(LoadCall::page))

            firstPage.complete(successPage("General", page = 1))
            advanceUntilIdle()

            assertEquals(listOf(1, 2), repository.calls.map(LoadCall::page))
            assertEquals(40, state.value.articles.size)
        }

    private fun createViewModel(repository: NewsRepository) =
        CategoriesViewModel(
            repository = repository,
            categoriesReducer = CategoriesReducer(),
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
        ): DataResult<NewsPage> =
            DataResult.Success(
                NewsPage(
                    articles = successArticles(prefix, page, articleCount),
                    totalResults = totalResults,
                ),
            )

        fun successArticles(
            prefix: String,
            page: Int,
            articleCount: Int = 20,
        ): List<Article> {
            val start = (page - 1) * 20 + 1
            return (start until start + articleCount).map { article(prefix, it) }
        }

        fun failure(): DataResult<NewsPage> = DataResult.Failure(NetworkError.NoInternet)

        fun completed(result: DataResult<NewsPage>) =
            CompletableDeferred<DataResult<NewsPage>>().apply { complete(result) }
    }
}
