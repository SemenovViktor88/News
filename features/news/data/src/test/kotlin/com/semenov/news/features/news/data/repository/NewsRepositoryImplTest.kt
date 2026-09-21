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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsRepositoryImplTest {
    @Test
    fun `page one success clears and rewrites only its context`() = runTest {
        val local = FakeLocalDataSource()
        val request = NewsRequest.Search(" Android ")
        local.seed(request, listOf(article("Old")))
        val repository = repository(FakeRemoteDataSource(success(article("New"))), local)

        val result = repository.loadPage(request, page = 1)

        assertTrue(result is DataResult.Success)
        assertEquals(listOf(article("New")), repository.cachedArticles(request))
        assertEquals(listOf(StoreCall("search:android", 1)), local.storeCalls)
    }

    @Test
    fun `page two success appends after page one`() = runTest {
        val local = FakeLocalDataSource()
        val request = NewsRequest.Feed
        local.seed(request, listOf(article("Page 1")))
        val repository = repository(FakeRemoteDataSource(success(article("Page 2"))), local)

        repository.loadPage(request, page = 2)

        assertEquals(
            listOf(article("Page 1"), article("Page 2")),
            repository.cachedArticles(request),
        )
    }

    @Test
    fun `remote failure keeps cached articles untouched`() = runTest {
        val local = FakeLocalDataSource()
        val request = NewsRequest.Category("technology")
        val cached = listOf(article("Cached"))
        local.seed(request, cached)
        val repository = repository(FakeRemoteDataSource(failure()), local)

        val result = repository.loadPage(request, page = 1)

        assertEquals(failure(), result)
        assertEquals(cached, repository.cachedArticles(request))
        assertTrue(local.storeCalls.isEmpty())
    }

    @Test
    fun `remote failure without cache returns error`() = runTest {
        val local = FakeLocalDataSource()
        val repository = repository(FakeRemoteDataSource(failure()), local)

        val result = repository.loadPage(NewsRequest.Feed, page = 1)

        assertEquals(failure(), result)
        assertTrue(repository.cachedArticles(NewsRequest.Feed).isEmpty())
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

        repository.loadPage(NewsRequest.Feed, page = 1)
        repository.loadPage(NewsRequest.Category("SPORTS"), page = 1)

        assertEquals(listOf(article("Feed")), repository.cachedArticles(NewsRequest.Feed))
        assertEquals(
            listOf(article("Sports")),
            repository.cachedArticles(NewsRequest.Category("sports")),
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
        assertTrue(local.storeCalls.isEmpty())
    }

    private fun repository(
        remote: NewsRemoteDataSourse,
        local: NewsLocalDataSourse,
    ) = NewsRepositoryImpl(remote, local, NoOpLogger)

    private class FakeLocalDataSource : NewsLocalDataSourse {
        private val rows = mutableMapOf<String, MutableList<Article>>()
        val storeCalls = mutableListOf<StoreCall>()

        override suspend fun articles(request: NewsRequest): List<Article> =
            rows[request.contextKey()].orEmpty().toList()

        override suspend fun storePage(
            request: NewsRequest,
            page: Int,
            articles: List<Article>,
        ) {
            val key = request.contextKey()
            storeCalls += StoreCall(key, page)
            if (page == 1) rows[key] = mutableListOf()
            rows.getOrPut(key, ::mutableListOf).apply { addAll(articles) }
        }

        fun seed(
            request: NewsRequest,
            articles: List<Article>,
        ) {
            rows[request.contextKey()] = articles.toMutableList()
        }
    }

    private class FakeRemoteDataSource(
        vararg responses: DataResult<NewsPage>,
    ) : NewsRemoteDataSourse {
        private val responses = ArrayDeque(responses.toList())

        override suspend fun getTopHeadlines(request: TopHeadlinesRequest): DataResult<NewsPage> =
            responses.removeFirst()

        override suspend fun searchNews(request: SearchNewsRequest): DataResult<NewsPage> =
            responses.removeFirst()

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
