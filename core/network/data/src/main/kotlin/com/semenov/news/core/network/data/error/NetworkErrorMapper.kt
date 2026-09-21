package com.semenov.news.core.network.data.error

import com.semenov.news.core.domain.model.NetworkError
import io.ktor.client.call.DoubleReceiveException
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.serialization.SerializationException

fun mapHttpError(
    status: HttpStatusCode,
    message: String?,
): NetworkError =
    when (status) {
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

fun mapApiErrorCode(
    code: String?,
    message: String?,
): NetworkError =
    when (code) {
        "apiKeyDisabled",
        "apiKeyInvalid",
        "apiKeyMissing",
        -> NetworkError.Unauthorized(message)

        "apiKeyExhausted",
        "rateLimited",
        -> NetworkError.RateLimited(message)

        "unexpectedError" -> NetworkError.ServerError(HttpStatusCode.InternalServerError.value, message)
        else -> NetworkError.BadRequest(message)
    }

fun Throwable.toNetworkError(): NetworkError =
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
