package com.semenov.news.features.auth.domain.model

/** Kept only in memory between the password and verification steps. */
data class AuthChallenge(val userName: String)
