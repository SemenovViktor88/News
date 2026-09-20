package com.semenov.news.core.domain

/** Any startup task should implement this and bind itself into the initializer set. */
interface AppInitializer {
    fun initialize()
}
