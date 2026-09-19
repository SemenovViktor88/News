package com.semenov.news.core.network.data.error

import com.semenov.news.core.domain.model.NetworkError
import com.semenov.news.core.network.data.model.NewsApiErrorDto
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class NetworkErrorMapperTest {
    @Test
    fun `maps supported HTTP statuses`() {
        assertTrue(mapHttpError(HttpStatusCode.BadRequest, null) is NetworkError.BadRequest)
        assertTrue(mapHttpError(HttpStatusCode.Unauthorized, null) is NetworkError.Unauthorized)
        assertTrue(mapHttpError(HttpStatusCode.TooManyRequests, null) is NetworkError.RateLimited)
        assertTrue(mapHttpError(HttpStatusCode.NotFound, null) is NetworkError.ClientError)
        assertTrue(mapHttpError(HttpStatusCode.InternalServerError, null) is NetworkError.ServerError)
    }

    @Test
    fun `keeps API error message without exposing provider code`() {
        val error =
            mapHttpError(
                HttpStatusCode.Unauthorized,
                NewsApiErrorDto(
                    status = "error",
                    code = "apiKeyInvalid",
                    message = "The API key is invalid.",
                ),
            )

        assertEquals(
            NetworkError.Unauthorized("The API key is invalid."),
            error,
        )
    }

    @Test
    fun `maps API error code when an error body arrives with success status`() {
        val error =
            mapApiErrorCode(
                NewsApiErrorDto(
                    status = "error",
                    code = "rateLimited",
                    message = "Try again later.",
                ),
            )

        assertEquals(NetworkError.RateLimited("Try again later."), error)
    }

    @Test
    fun `maps transport serialization and unknown exceptions`() {
        assertEquals(
            NetworkError.Timeout,
            HttpRequestTimeoutException("https://example.com", 1_000, null).toNetworkError(),
        )
        assertEquals(NetworkError.NoInternet, UnknownHostException().toNetworkError())
        assertTrue(SerializationException("Invalid JSON").toNetworkError() is NetworkError.Serialization)
        assertTrue(IllegalStateException("Unexpected").toNetworkError() is NetworkError.Unknown)
    }
}
