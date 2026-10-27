package com.semenov.news.core.ui.interactions.domain.model.modal

data class VerificationCodeState(
    val code: String = "",
    val isSubmitting: Boolean = false,
    val error: VerificationCodeError? = null,
    val isVerified: Boolean = false,
    val resendCount: Int = 0,
    val canResend: Boolean = true,
) {
    override fun toString(): String =
        "VerificationCodeState(isSubmitting=$isSubmitting, error=$error, isVerified=$isVerified, resendCount=$resendCount, canResend=$canResend)"
}

enum class VerificationCodeError {
    InvalidCode,
    Unavailable,
}
