package com.semenov.news.core.network.data.error

import com.semenov.news.core.domain.model.NetworkError
import com.semenov.news.core.network.data.model.NewsApiErrorDto
import io.ktor.client.call.DoubleReceiveException
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

internal fun mapHttpError(
    status: HttpStatusCode,
    error: NewsApiErrorDto?,
): NetworkError {
    val message = error?.message
    return when (status) {
        HttpStatusCode.BadRequest -> NetworkError.BadRequest(message)
        HttpStatusCode.Unauthorized -> NetworkError.Unauthorized(message)
        HttpStatusCode.TooManyRequests -> NetworkError.RateLimited(message)
        else -> if (status.value in 400..499) {
            NetworkError.ClientError(status.value, message)
        } else if (status.value in 500..599) {
            NetworkError.ServerError(status.value, message)
        } else {
            NetworkError.Unknown(message ?: "Unexpected HTTP status ${status.value}")
        }
    }
}

internal fun mapApiErrorCode(error: NewsApiErrorDto?): NetworkError =
    when (error?.code) {
        "apiKeyDisabled",
        "apiKeyInvalid",
        "apiKeyMissing",
        -> NetworkError.Unauthorized(error.message)

        "apiKeyExhausted",
        "rateLimited",
        -> NetworkError.RateLimited(error.message)

        "unexpectedError" -> NetworkError.ServerError(HttpStatusCode.InternalServerError.value, error.message)
        else -> NetworkError.BadRequest(error?.message)
    }

internal fun Throwable.toNetworkError(): NetworkError =
    when (this) {
        is HttpRequestTimeoutException,
        is ConnectTimeoutException,
        is SocketTimeoutException,
        -> NetworkError.Timeout

        is UnknownHostException,
        is IOException,
        -> NetworkError.NoInternet

        is JsonConvertException,
        is SerializationException,
        is NoTransformationFoundException,
        is DoubleReceiveException,
        -> NetworkError.Serialization(message)

        else -> NetworkError.Unknown(message)
    }
