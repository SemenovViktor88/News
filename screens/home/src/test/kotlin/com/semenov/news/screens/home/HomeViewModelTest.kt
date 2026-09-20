package com.semenov.news.screens.home

import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.domain.model.DataResult
import com.semenov.news.core.domain.model.NewsPage
import com.semenov.news.core.domain.model.NewsSource
import com.semenov.news.core.domain.model.SearchNewsRequest
import com.semenov.news.core.domain.model.SourcesRequest
import com.semenov.news.core.domain.model.TopHeadlinesRequest
import com.semenov.news.features.news.domain.repository.NewsRepository
import com.semenov.news.screens.home.reducer.HomeReducer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

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
    fun `initialization loads home headlines`() = runTest(dispatcher) {
        val repository = RecordingNewsRepository()
        val viewModel =
            HomeViewModel(
                repository = repository,
                homeReducer = HomeReducer(),
                logger = NoOpLogger,
            )

        val state = viewModel.state
        advanceUntilIdle()

        assertEquals(listOf(TopHeadlinesRequest(country = "us")), repository.requests)
        assertEquals(true, state.value.hasLoaded)
    }

    private class RecordingNewsRepository : NewsRepository {
        val requests = mutableListOf<TopHeadlinesRequest>()

        override suspend fun getTopHeadlines(request: TopHeadlinesRequest): DataResult<NewsPage> {
            requests += request
            return DataResult.Success(NewsPage(emptyList(), 0))
        }

        override suspend fun searchNews(request: SearchNewsRequest): DataResult<NewsPage> =
            error("Not expected in initialization test")

        override suspend fun getSources(request: SourcesRequest): DataResult<List<NewsSource>> =
            error("Not expected in initialization test")
    }

    private object NoOpLogger : AppLogger {
        override fun log(
            tag: String,
            message: String,
            t: Throwable?,
        ) = Unit
    }
}
