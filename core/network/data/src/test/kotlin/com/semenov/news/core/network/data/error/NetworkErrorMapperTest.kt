package com.semenov.news.core.network.data.error

import com.semenov.news.core.domain.model.NetworkError
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import java.net.UnknownHostException
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun `keeps error message`() {
        assertEquals(
            NetworkError.Unauthorized("The API key is invalid."),
            mapHttpError(HttpStatusCode.Unauthorized, "The API key is invalid."),
        )
    }

    @Test
    fun `maps API error code`() {
        assertEquals(
            NetworkError.RateLimited("Try again later."),
            mapApiErrorCode(
                code = "rateLimited",
                message = "Try again later.",
            ),
        )
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
