package com.semenov.news.features.auth.data.verification

internal interface AuthCodeNotifier {
    /** Returns false when system or channel notification delivery is disabled. */
    fun show(code: String): Boolean

    fun clear()
}
