package com.semenov.news.core.network.data.datasource

import com.semenov.news.core.domain.model.DataResult
import com.semenov.news.core.domain.model.NewsPage
import com.semenov.news.core.domain.model.NetworkError
import com.semenov.news.core.domain.model.NewsSource
import com.semenov.news.core.domain.model.SearchNewsRequest
import com.semenov.news.core.domain.model.SourcesRequest
import com.semenov.news.core.domain.model.TopHeadlinesRequest
import com.semenov.news.core.network.data.error.mapApiErrorCode
import com.semenov.news.core.network.data.error.mapHttpError
import com.semenov.news.core.network.data.error.toNetworkError
import com.semenov.news.core.network.data.mapper.toDomain
import com.semenov.news.core.network.data.model.NewsApiErrorDto
import com.semenov.news.core.network.data.model.NewsResponseDto
import com.semenov.news.core.network.data.model.SourcesResponseDto
import com.semenov.news.core.network.domain.ApiContract
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class NewsApiDataSource @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun getTopHeadlines(request: TopHeadlinesRequest): DataResult<NewsPage> =
        executeRequest(
            request = {
                httpClient.get(ApiContract.Endpoints.TOP_HEADLINES) {
                    parameterIfPresent(ApiContract.Query.COUNTRY, request.country)
                    parameterIfPresent(ApiContract.Query.CATEGORY, request.category?.apiValue)
                    parameterIfPresent(ApiContract.Query.SEARCH, request.query)
                    parameter(ApiContract.Query.PAGE, request.page)
                    parameter(ApiContract.Query.PAGE_SIZE, request.pageSize)
                }
            },
            parse = { response ->
                response.body<NewsResponseDto>().let { body ->
                    ApiBody(
                        status = body.status,
                        code = body.code,
                        message = body.message,
                        data = body.toDomain(),
                    )
                }
            },
        )

    suspend fun searchNews(request: SearchNewsRequest): DataResult<NewsPage> =
        executeRequest(
            request = {
                httpClient.get(ApiContract.Endpoints.EVERYTHING) {
                    parameter(ApiContract.Query.SEARCH, request.query)
                    parameter(ApiContract.Query.PAGE, request.page)
                    parameter(ApiContract.Query.PAGE_SIZE, request.pageSize)
                }
            },
            parse = { response ->
                response.body<NewsResponseDto>().let { body ->
                    ApiBody(
                        status = body.status,
                        code = body.code,
                        message = body.message,
                        data = body.toDomain(),
                    )
                }
            },
        )

    suspend fun getSources(request: SourcesRequest = SourcesRequest()): DataResult<List<NewsSource>> =
        executeRequest(
            request = {
                httpClient.get(ApiContract.Endpoints.SOURCES) {
                    parameterIfPresent(ApiContract.Query.CATEGORY, request.category?.apiValue)
                    parameterIfPresent(ApiContract.Query.LANGUAGE, request.language)
                    parameterIfPresent(ApiContract.Query.COUNTRY, request.country)
                }
            },
            parse = { response ->
                response.body<SourcesResponseDto>().let { body ->
                    ApiBody(
                        status = body.status,
                        code = body.code,
                        message = body.message,
                        data = body.toDomain(),
                    )
                }
            },
        )

    private suspend fun <T> executeRequest(
        request: suspend () -> HttpResponse,
        parse: suspend (HttpResponse) -> ApiBody<T>,
    ): DataResult<T> =
        try {
            val response = request()
            if (!response.status.isSuccess()) {
                val error = response.errorBodyOrNull()
                DataResult.Failure(mapHttpError(response.status, error))
            } else {
                val body = parse(response)
                when (body.status) {
                    API_STATUS_OK -> DataResult.Success(body.data)
                    API_STATUS_ERROR ->
                        DataResult.Failure(
                            mapApiErrorCode(
                                NewsApiErrorDto(
                                    status = body.status,
                                    code = body.code,
                                    message = body.message,
                                ),
                            ),
                        )

                    else -> DataResult.Failure(NetworkError.Serialization("Missing or invalid API response status"))
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            DataResult.Failure(throwable.toNetworkError())
        }

    private suspend fun HttpResponse.errorBodyOrNull(): NewsApiErrorDto? =
        try {
            body()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            null
        }

    private fun io.ktor.client.request.HttpRequestBuilder.parameterIfPresent(
        key: String,
        value: String?,
    ) {
        value?.takeIf(String::isNotBlank)?.let { parameter(key, it) }
    }

    private companion object {
        const val API_STATUS_OK = "ok"
        const val API_STATUS_ERROR = "error"
    }

    private data class ApiBody<T>(
        val status: String?,
        val code: String?,
        val message: String?,
        val data: T,
    )
}
