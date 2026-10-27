package com.semenov.news.features.auth.data.verification

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureAuthCodeGeneratorTest {
    private val generator = SecureAuthCodeGenerator()

    @Test
    fun `generated codes are exactly four digits and never repeat the excluded value`() {
        var previous = "0000"
        repeat(200) {
            val code = generator.generate(excluding = previous)
            assertTrue(code.matches(Regex("[0-9]{4}")))
            assertNotEquals(previous, code)
            previous = code
        }
    }

    @Test
    fun `first code is four digits without a previous value`() {
        assertTrue(generator.generate(excluding = null).matches(Regex("[0-9]{4}")))
    }
}
