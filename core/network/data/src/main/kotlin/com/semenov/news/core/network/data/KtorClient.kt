package com.semenov.news.core.network.data

import com.semenov.news.core.domain.AppInfoProvider
import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.network.domain.ApiContract
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val TIMEOUT_MILLIS = 30_000L

internal fun initHttpClient(
    appLogger: AppLogger,
    infoProvider: AppInfoProvider,
): HttpClient = HttpClient(OkHttp) {
    expectSuccess = false

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            },
        )
    }

    install(HttpTimeout) {
        requestTimeoutMillis = TIMEOUT_MILLIS
        connectTimeoutMillis = TIMEOUT_MILLIS
        socketTimeoutMillis = TIMEOUT_MILLIS
    }

    val logLevel = LogLevel.ALL.takeIf { infoProvider.isDebug } ?: LogLevel.NONE

    install(Logging) {
        level = logLevel
        logger = KtorLogger(appLogger)
        sanitizeHeader { header ->
            header.equals(ApiContract.Headers.API_KEY, ignoreCase = true)
        }
    }

    defaultRequest {
        url("${infoProvider.baseUrl.trimEnd('/')}/")
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.Json)
        header(ApiContract.Headers.API_KEY, infoProvider.apiKey)
    }
}
