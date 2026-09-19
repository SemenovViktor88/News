package com.semenov.news.core.domain

interface AppLogger {
    fun log(
        tag: String,
        message: String,
        t: Throwable? = null,
    )
}
