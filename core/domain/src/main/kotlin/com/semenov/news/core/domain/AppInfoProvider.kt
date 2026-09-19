package com.semenov.news.core.domain

interface AppInfoProvider {
    val deviceName: String
    val baseUrl: String
    val apiKey: String
    val isDebug: Boolean
}