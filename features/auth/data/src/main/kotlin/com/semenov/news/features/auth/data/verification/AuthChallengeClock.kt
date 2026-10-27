package com.semenov.news.features.auth.data.verification

import android.os.SystemClock
import javax.inject.Inject

internal fun interface AuthChallengeClock {
    fun nowMillis(): Long
}

internal class ElapsedRealtimeAuthChallengeClock @Inject constructor() : AuthChallengeClock {
    override fun nowMillis(): Long = SystemClock.elapsedRealtime()
}
