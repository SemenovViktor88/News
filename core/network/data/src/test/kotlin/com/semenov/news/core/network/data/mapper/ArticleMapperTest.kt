package com.semenov.news.core.network.data.mapper

import com.semenov.news.core.domain.model.NewsCategory
import com.semenov.news.core.network.data.model.ArticleDto
import com.semenov.news.core.network.data.model.NewsResponseDto
import com.semenov.news.core.network.data.model.SourceDto
import com.semenov.news.core.network.data.model.SourcesResponseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArticleMapperTest {
    @Test
    fun `maps a complete article and renames image field`() {
        val dto =
            ArticleDto(
                source = SourceDto(id = "source-id", name = "Source name"),
                author = "Author",
                title = "Title",
                description = "Description",
                url = "https://example.com/article",
                urlToImage = "https://example.com/image.jpg",
                publishedAt = "2026-09-17T10:00:00Z",
                content = "Content",
            )

        val article = dto.toDomain()

        assertEquals("Source name", article.source)
        assertEquals("Author", article.author)
        assertEquals("Title", article.title)
        assertEquals("Description", article.description)
        assertEquals("https://example.com/article", article.url)
        assertEquals("https://example.com/image.jpg", article.imageUrl)
        assertEquals("2026-09-17T10:00:00Z", article.publishedAt)
        assertEquals("Content", article.content)
    }

    @Test
    fun `preserves missing optional article data as null`() {
        val article = ArticleDto().toDomain()

        assertNull(article.source)
        assertNull(article.author)
        assertNull(article.title)
        assertNull(article.description)
        assertNull(article.url)
        assertNull(article.imageUrl)
        assertNull(article.publishedAt)
        assertNull(article.content)
    }

    @Test
    fun `maps response articles and prevents a negative total`() {
        val page =
            NewsResponseDto(
                status = "ok",
                totalResults = -1,
                articles = listOf(ArticleDto(title = "First"), ArticleDto(title = "Second")),
            ).toDomain()

        assertEquals(0, page.totalResults)
        assertEquals(listOf("First", "Second"), page.articles.map { it.title })
    }

    @Test
    fun `maps sources response to domain sources`() {
        val sources =
            SourcesResponseDto(
                status = "ok",
                sources =
                    listOf(
                        SourceDto(
                            id = "bbc-news",
                            name = "BBC News",
                            description = "BBC news source",
                            url = "https://www.bbc.co.uk/news",
                            category = "general",
                            language = "en",
                            country = "gb",
                        ),
                    ),
            ).toDomain()

        assertEquals(1, sources.size)
        assertEquals("bbc-news", sources.single().id)
        assertEquals("BBC News", sources.single().name)
        assertEquals(NewsCategory.GENERAL, sources.single().category)
        assertEquals("en", sources.single().language)
        assertEquals("gb", sources.single().country)
    }

    @Test
    fun `maps unknown source category to null`() {
        val source = SourceDto(category = "unknown").toDomain()

        assertNull(source.category)
    }
}
