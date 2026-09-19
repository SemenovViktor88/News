package com.semenov.news.core.network.data

import com.semenov.news.core.domain.AppLogger
import io.ktor.client.plugins.logging.Logger

internal class KtorLogger(
    private val appLogger: AppLogger,
) : Logger {
    override fun log(message: String) {
        message.chunked(MAX_LOG_LENGTH).forEach { chunk ->
            appLogger.log(KTOR_LOG_TAG, chunk)
        }
    }

    private companion object {
        const val KTOR_LOG_TAG = "KTOR"
        const val MAX_LOG_LENGTH = 2_000
    }
}
