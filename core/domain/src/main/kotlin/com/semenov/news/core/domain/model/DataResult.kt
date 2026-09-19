package com.semenov.news.core.domain.model

sealed interface DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>

    data class Failure(val error: NetworkError) : DataResult<Nothing>
}

sealed interface NetworkError {
    data class BadRequest(val message: String?) : NetworkError

    data class Unauthorized(val message: String?) : NetworkError

    data class RateLimited(val message: String?) : NetworkError

    data class ClientError(
        val statusCode: Int,
        val message: String?,
    ) : NetworkError

    data class ServerError(
        val statusCode: Int,
        val message: String?,
    ) : NetworkError

    data object Timeout : NetworkError

    data object NoInternet : NetworkError

    data class Serialization(val message: String?) : NetworkError

    data class Unknown(val message: String?) : NetworkError
}
