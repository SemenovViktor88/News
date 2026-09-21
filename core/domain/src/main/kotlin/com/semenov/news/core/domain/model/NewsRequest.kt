package com.semenov.news.core.domain.model

private const val MAX_PAGE_SIZE = 100

sealed interface NewsRequest {
    data object Feed : NewsRequest

    data class Search(val query: String) : NewsRequest {
        init {
            require(query.isNotBlank()) { "Search query must not be blank" }
        }
    }

    data class Category(val category: String) : NewsRequest {
        init {
            require(category.isNotBlank()) { "Category must not be blank" }
        }
    }
}

enum class NewsCategory(val apiValue: String) {
    BUSINESS("business"),
    ENTERTAINMENT("entertainment"),
    GENERAL("general"),
    HEALTH("health"),
    SCIENCE("science"),
    SPORTS("sports"),
    TECHNOLOGY("technology"),
}

data class TopHeadlinesRequest(
    val country: String? = null,
    val category: NewsCategory? = null,
    val query: String? = null,
    val page: Int = 1,
    val pageSize: Int = 20,
) {
    init {
        require(page > 0) { "Page must be greater than zero" }
        require(pageSize in 1..MAX_PAGE_SIZE) { "Page size must be between 1 and $MAX_PAGE_SIZE" }
    }
}

data class SearchNewsRequest(
    val query: String,
    val page: Int = 1,
    val pageSize: Int = 20,
) {
    init {
        require(query.isNotBlank()) { "Search query must not be blank" }
        require(page > 0) { "Page must be greater than zero" }
        require(pageSize in 1..MAX_PAGE_SIZE) { "Page size must be between 1 and $MAX_PAGE_SIZE" }
    }
}

data class SourcesRequest(
    val category: NewsCategory? = null,
    val language: String? = null,
    val country: String? = null,
) {
    init {
        require(language == null || language.length == ISO_CODE_LENGTH) {
            "Language must be a two-letter ISO 639-1 code"
        }
        require(country == null || country.length == ISO_CODE_LENGTH) {
            "Country must be a two-letter ISO 3166-1 code"
        }
    }

    private companion object {
        const val ISO_CODE_LENGTH = 2
    }
}
