package com.semenov.news.core.network.data

import com.semenov.news.core.domain.AppInfoProvider
import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.domain.model.NetworkError
import com.semenov.news.core.network.data.error.mapHttpError
import com.semenov.news.core.network.data.error.toNetworkError
import com.semenov.news.core.network.domain.ApiContract
import com.sun.net.httpserver.HttpServer
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import java.io.Closeable
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Exercises the production OkHttp engine and plugins without contacting the news service. */
class KtorClientIntegrationTest {
    @Test(timeout = 15_000)
    fun `real request preserves base path headers and query and decodes JSON with new fields`() = runBlocking {
        LocalJsonServer(
            body = """{"first":"ready","second":7,"futureField":{"ignored":true}}""",
        ).use { server ->
            val logger = RecordingLogger()
            val client = initHttpClient(logger, testInfo(server.baseUrl, isDebug = true))
            try {
                // Pair has a built-in serializer, so the response still passes through the real
                // ContentNegotiation plugin without a test-only serialization compiler plugin.
                val result = client.get(ApiContract.Endpoints.TOP_HEADLINES) {
                    parameter(ApiContract.Query.SEARCH, "новини + Kotlin")
                    parameter(ApiContract.Query.PAGE, 2)
                }.body<Pair<String, Int>>()

                assertEquals("ready" to 7, result)
                val request = server.takeRequest()
                assertEquals("GET", request.method)
                assertEquals("/v2/top-headlines", request.uri.path)
                assertEquals("новини + Kotlin", request.query[ApiContract.Query.SEARCH])
                assertEquals("2", request.query[ApiContract.Query.PAGE])
                assertEquals(TEST_API_KEY, request.header(ApiContract.Headers.API_KEY))
                assertEquals("application/json", request.header("Accept"))
                assertTrue(request.header("Content-Type").orEmpty().startsWith("application/json"))

                val logs = logger.messages.joinToString("\n")
                assertTrue("Debug logging should include the sanitized API key header", logs.contains(ApiContract.Headers.API_KEY, ignoreCase = true))
                assertFalse("The credential must never appear in debug logs", logs.contains(TEST_API_KEY))
            } finally {
                client.close()
            }
        }
    }

    @Test(timeout = 15_000)
    fun `invalid response JSON is mapped to serialization error by production mapper`() = runBlocking {
        LocalJsonServer(body = """{"first":"ready","second":not-a-number}""").use { server ->
            val client = initHttpClient(RecordingLogger(), testInfo(server.baseUrl))
            try {
                val failure = runCatching {
                    client.get(ApiContract.Endpoints.EVERYTHING).body<Pair<String, Int>>()
                }.exceptionOrNull()

                assertNotNull("Invalid JSON must fail in ContentNegotiation", failure)
                assertTrue(failure!!.toNetworkError() is NetworkError.Serialization)
                assertEquals("/v2/everything", server.takeRequest().uri.path)
            } finally {
                client.close()
            }
        }
    }

    @Test(timeout = 15_000)
    fun `HTTP errors reach application mapper and release client does not log requests`() = runBlocking {
        LocalJsonServer(status = 429, body = """{"message":"Try again later"}""").use { server ->
            val logger = RecordingLogger()
            val client = initHttpClient(logger, testInfo(server.baseUrl))
            try {
                val response = client.get(ApiContract.Endpoints.SOURCES)

                assertEquals(HttpStatusCode.TooManyRequests, response.status)
                assertEquals(
                    NetworkError.RateLimited("Try again later"),
                    mapHttpError(response.status, "Try again later"),
                )
                assertEquals("/v2/top-headlines/sources", server.takeRequest().uri.path)
                assertTrue("Release requests must not be logged", logger.messages.isEmpty())
            } finally {
                client.close()
            }
        }
    }

    private fun testInfo(serverUrl: String, isDebug: Boolean = false) = object : AppInfoProvider {
        override val deviceName = "Ktor integration test"
        override val baseUrl = "$serverUrl/v2"
        override val apiKey = TEST_API_KEY
        override val isDebug = isDebug
    }

    private class RecordingLogger : AppLogger {
        val messages = CopyOnWriteArrayList<String>()

        override fun log(tag: String, message: String, t: Throwable?) {
            messages += message
        }
    }

    private class LocalJsonServer(status: Int = 200, body: String) : Closeable {
        private val requests = LinkedBlockingQueue<RecordedRequest>()
        private val executor = Executors.newSingleThreadExecutor { task ->
            Thread(task, "ktor-test-http-server").apply { isDaemon = true }
        }
        private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            this.executor = this@LocalJsonServer.executor
            createContext("/") { exchange ->
                try {
                    requests.offer(
                        RecordedRequest(
                            method = exchange.requestMethod,
                            uri = exchange.requestURI,
                            headers = exchange.requestHeaders.entries.associate { (name, values) ->
                                name to values.toList()
                            },
                        ),
                    )
                    val bytes = body.toByteArray(Charsets.UTF_8)
                    exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
                    exchange.sendResponseHeaders(status, bytes.size.toLong())
                    exchange.responseBody.use { it.write(bytes) }
                } finally {
                    exchange.close()
                }
            }
            start()
        }

        val baseUrl: String = "http://127.0.0.1:${server.address.port}"

        fun takeRequest(): RecordedRequest = checkNotNull(requests.poll(5, TimeUnit.SECONDS)) {
            "The production HTTP client did not reach the local server"
        }

        override fun close() {
            server.stop(0)
            executor.shutdownNow()
            check(executor.awaitTermination(5, TimeUnit.SECONDS)) {
                "The local HTTP server did not stop"
            }
        }
    }

    private data class RecordedRequest(
        val method: String,
        val uri: URI,
        val headers: Map<String, List<String>>,
    ) {
        val query: Map<String, String>
            get() = uri.rawQuery.orEmpty().split('&').filter(String::isNotEmpty).associate { entry ->
                val pair = entry.split('=', limit = 2)
                URLDecoder.decode(pair[0], "UTF-8") to URLDecoder.decode(pair.getOrElse(1) { "" }, "UTF-8")
            }

        fun header(name: String): String? = headers.entries
            .firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.joinToString(", ")
    }

    private companion object {
        const val TEST_API_KEY = "local-test-credential-never-log"
    }
}
