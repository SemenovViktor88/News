package com.semenov.news.features.auth.data.verification

import java.security.SecureRandom
import javax.inject.Inject

internal fun interface AuthCodeGenerator {
    fun generate(excluding: String?): String
}

internal class SecureAuthCodeGenerator @Inject constructor() : AuthCodeGenerator {
    private val random = SecureRandom()

    override fun generate(excluding: String?): String {
        val excludedNumber = excluding?.toIntOrNull()?.takeIf { it in 0..9_999 }
        val number = if (excludedNumber == null) {
            random.nextInt(10_000)
        } else {
            // Pick uniformly from the remaining 9,999 values, without retry loops.
            val candidate = random.nextInt(9_999)
            if (candidate >= excludedNumber) candidate + 1 else candidate
        }
        return number.toString().padStart(4, '0')
    }
}
