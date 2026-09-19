package com.semenov.news.screens.categories

import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.domain.model.Article
import com.semenov.news.core.domain.model.DataResult
import com.semenov.news.core.domain.model.NetworkError
import com.semenov.news.core.domain.model.NewsCategory
import com.semenov.news.core.domain.model.NewsPage
import com.semenov.news.core.domain.model.NewsSource
import com.semenov.news.core.domain.model.SearchNewsRequest
import com.semenov.news.core.domain.model.SourcesRequest
import com.semenov.news.core.domain.model.TopHeadlinesRequest
import com.semenov.news.core.domain.repository.NewsRepository
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
    fun `initialization loads general category and exposes loading state`() = runTest(dispatcher) {
        val pendingResult = CompletableDeferred<DataResult<NewsPage>>()
        val repository = FakeNewsRepository(response = { pendingResult.await() })
        val viewModel = createViewModel(repository)

        val state = viewModel.state
        runCurrent()

        assertEquals(NewsCategory.GENERAL, state.value.selectedCategory)
        assertTrue(state.value.isLoading)
        assertEquals(NewsCategory.GENERAL, repository.requests.single().category)
        assertEquals("us", repository.requests.single().country)

        pendingResult.complete(success(ARTICLE_GENERAL))
        advanceUntilIdle()

        assertEquals(listOf(ARTICLE_GENERAL), state.value.articles)
        assertTrue(state.value.hasLoaded)
        assertFalse(state.value.isLoading)
    }

    @Test
    fun `category selection keeps content while loading and replaces it on success`() =
        runTest(dispatcher) {
            val categoryResult = CompletableDeferred<DataResult<NewsPage>>()
            val repository =
                FakeNewsRepository(
                    responses =
                        ArrayDeque(
                            listOf(
                                completed(success(ARTICLE_GENERAL)),
                                categoryResult,
                            ),
                        ),
                )
            val viewModel = createViewModel(repository)
            val state = viewModel.state
            advanceUntilIdle()

            viewModel.processIntent(CategoriesIntent.CategorySelected(NewsCategory.TECHNOLOGY))
            runCurrent()

            assertEquals(NewsCategory.TECHNOLOGY, state.value.selectedCategory)
            assertEquals(listOf(ARTICLE_GENERAL), state.value.articles)
            assertTrue(state.value.isSwitchingCategory)
            assertFalse(state.value.isLoading)
            assertEquals(NewsCategory.TECHNOLOGY, repository.requests.last().category)

            categoryResult.complete(success(ARTICLE_TECHNOLOGY))
            advanceUntilIdle()

            assertEquals(listOf(ARTICLE_TECHNOLOGY), state.value.articles)
            assertFalse(state.value.isSwitchingCategory)
            assertNull(state.value.error)
        }

    @Test
    fun `successful empty response produces loaded empty state`() = runTest(dispatcher) {
        val repository = FakeNewsRepository(response = { success() })
        val viewModel = createViewModel(repository)

        val state = viewModel.state
        advanceUntilIdle()

        assertTrue(state.value.hasLoaded)
        assertTrue(state.value.articles.isEmpty())
        assertFalse(state.value.isLoading)
        assertNull(state.value.error)
    }

    @Test
    fun `initial failure exposes typed error`() = runTest(dispatcher) {
        val repository =
            FakeNewsRepository(response = { DataResult.Failure(NetworkError.NoInternet) })
        val viewModel = createViewModel(repository)

        val state = viewModel.state
        advanceUntilIdle()

        assertFalse(state.value.hasLoaded)
        assertFalse(state.value.isLoading)
        assertEquals(NetworkError.NoInternet, state.value.error)
    }

    @Test
    fun `retry reloads the currently selected category`() = runTest(dispatcher) {
        val repository =
            FakeNewsRepository(
                responses =
                    ArrayDeque(
                        listOf(
                            completed(success(ARTICLE_GENERAL)),
                            completed(DataResult.Failure(NetworkError.Timeout)),
                            completed(success(ARTICLE_SPORTS)),
                        ),
                    ),
            )
        val viewModel = createViewModel(repository)
        val state = viewModel.state
        advanceUntilIdle()

        viewModel.processIntent(CategoriesIntent.CategorySelected(NewsCategory.SPORTS))
        advanceUntilIdle()

        assertEquals(NewsCategory.SPORTS, state.value.selectedCategory)
        assertEquals(NetworkError.Timeout, state.value.error)
        assertEquals(listOf(ARTICLE_GENERAL), state.value.articles)

        viewModel.processIntent(CategoriesIntent.Retry)
        advanceUntilIdle()

        assertEquals(
            listOf(NewsCategory.GENERAL, NewsCategory.SPORTS, NewsCategory.SPORTS),
            repository.requests.map(TopHeadlinesRequest::category),
        )
        assertEquals(listOf(ARTICLE_SPORTS), state.value.articles)
        assertNull(state.value.error)
    }

    private fun createViewModel(repository: NewsRepository) =
        CategoriesViewModel(
            repository = repository,
            categoriesReducer = CategoriesReducer(),
            logger = NoOpLogger,
        )

    private class FakeNewsRepository(
        private val response: (suspend (TopHeadlinesRequest) -> DataResult<NewsPage>)? = null,
        private val responses: ArrayDeque<CompletableDeferred<DataResult<NewsPage>>> = ArrayDeque(),
    ) : NewsRepository {
        val requests = mutableListOf<TopHeadlinesRequest>()

        override suspend fun getTopHeadlines(request: TopHeadlinesRequest): DataResult<NewsPage> {
            requests += request
            return response?.invoke(request) ?: responses.removeFirst().await()
        }

        override suspend fun searchNews(request: SearchNewsRequest): DataResult<NewsPage> =
            error("Not expected in categories tests")

        override suspend fun getSources(request: SourcesRequest): DataResult<List<NewsSource>> =
            error("Not expected in categories tests")
    }

    private object NoOpLogger : AppLogger {
        override fun log(
            tag: String,
            message: String,
            t: Throwable?,
        ) = Unit
    }

    private companion object {
        val ARTICLE_GENERAL = article("General")
        val ARTICLE_TECHNOLOGY = article("Technology")
        val ARTICLE_SPORTS = article("Sports")

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
            DataResult.Success(NewsPage(articles.toList(), articles.size))

        fun completed(result: DataResult<NewsPage>) =
            CompletableDeferred<DataResult<NewsPage>>().apply { complete(result) }
    }
}
