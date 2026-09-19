package com.semenov.news.core.network.domain

object ApiContract {
    object Endpoints {
        const val EVERYTHING = "everything"
        const val TOP_HEADLINES = "top-headlines"
        const val SOURCES = "$TOP_HEADLINES/sources"
    }

    object Headers {
        const val API_KEY = "X-Api-Key"
    }

    object Query {
        const val COUNTRY = "country"
        const val CATEGORY = "category"
        const val LANGUAGE = "language"
        const val SEARCH = "q"
        const val PAGE = "page"
        const val PAGE_SIZE = "pageSize"
    }
}
