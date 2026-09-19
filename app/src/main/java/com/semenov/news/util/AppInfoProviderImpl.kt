package com.semenov.news.util

import android.os.Build
import com.semenov.news.BuildConfig
import com.semenov.news.core.domain.AppInfoProvider

class AppInfoProviderImpl : AppInfoProvider {
    private val manufacturer = Build.MANUFACTURER
    private val model = Build.MODEL
    override val deviceName: String =
        if (model.startsWith(manufacturer)) model else "$manufacturer $model"
    override val baseUrl: String = BuildConfig.BASE_URL
    override val apiKey: String = BuildConfig.API_KEY
    override val isDebug: Boolean = BuildConfig.DEBUG
}
