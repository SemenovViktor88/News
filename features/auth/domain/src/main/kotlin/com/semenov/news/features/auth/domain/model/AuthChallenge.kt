package com.semenov.news.features.auth.domain.model

import java.util.UUID

/** Kept only in memory between the password and verification steps. */
data class AuthChallenge(
    val userName: String,
    val id: String = UUID.randomUUID().toString(),
)
