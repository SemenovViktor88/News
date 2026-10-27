package com.semenov.news.screens.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class UserInitialTest {
    @Test
    fun `initial uses the first complete unicode character in uppercase`() {
        assertEquals("В", "віктор".avatarInitial())
        assertEquals("A", " anna ".avatarInitial())
        assertEquals("\uD801\uDC00", "\uD801\uDC28name".avatarInitial())
        assertEquals("?", "  ".avatarInitial())
    }
}
