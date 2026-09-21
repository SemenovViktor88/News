package com.semenov.news.core.db.mapper

import com.semenov.news.core.domain.model.Article
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArticleEntityMapperTest {
    @Test
    fun `article entity round trip preserves article and cache metadata`() {
        val article =
            Article(
                source = "Source",
                author = "Author",
                title = "Title",
                description = "Description",
                url = "https://example.com/article",
                imageUrl = "https://example.com/image.jpg",
                publishedAt = "2026-09-20T10:00:00Z",
                content = "Content",
            )

        val entity =
            requireNotNull(
                article.toEntityOrNull(
                    contextKey = "search:android",
                    page = 2,
                    position = 7,
                    cachedAt = 1234L,
                ),
            )

        assertEquals("search:android", entity.contextKey)
        assertEquals(2, entity.page)
        assertEquals(7, entity.position)
        assertEquals(1234L, entity.cachedAt)
        assertEquals(article, entity.toDomain())
    }

    @Test
    fun `article without stable url is not cached`() {
        val article =
            Article(
                source = null,
                author = null,
                title = "Title",
                description = null,
                url = null,
                imageUrl = null,
                publishedAt = null,
                content = null,
            )

        assertNull(
            article.toEntityOrNull(
                contextKey = "feed",
                page = 1,
                position = 0,
                cachedAt = 1234L,
            ),
        )
    }
}
