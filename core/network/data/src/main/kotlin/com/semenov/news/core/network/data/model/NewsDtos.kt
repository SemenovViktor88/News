package com.semenov.news.core.network.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class NewsResponseDto(
    val status: String? = null,
    val totalResults: Int = 0,
    val articles: List<ArticleDto> = emptyList(),
    val code: String? = null,
    val message: String? = null,
)

@Serializable
internal data class ArticleDto(
    val source: SourceDto? = null,
    val author: String? = null,
    val title: String? = null,
    val description: String? = null,
    val url: String? = null,
    @SerialName("urlToImage")
    val urlToImage: String? = null,
    val publishedAt: String? = null,
    val content: String? = null,
)

@Serializable
internal data class SourceDto(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val url: String? = null,
    val category: String? = null,
    val language: String? = null,
    val country: String? = null,
)

@Serializable
internal data class SourcesResponseDto(
    val status: String? = null,
    val sources: List<SourceDto> = emptyList(),
    val code: String? = null,
    val message: String? = null,
)

@Serializable
internal data class NewsApiErrorDto(
    val status: String? = null,
    val code: String? = null,
    val message: String? = null,
)
