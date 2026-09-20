package com.semenov.core.logging

import com.semenov.news.core.domain.AppInfoProvider
import com.semenov.news.core.domain.AppInitializer
import timber.log.Timber
import javax.inject.Inject

class TimberInitializer @Inject constructor(
    private val infoProvider: AppInfoProvider,
) : AppInitializer {
    override fun initialize() {
        if (infoProvider.isDebug) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
