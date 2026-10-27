package com.semenov.news.core.ui.interactions.domain.model.modal

import java.util.UUID
import kotlinx.coroutines.flow.StateFlow

sealed class ModalDestination {
    open val id: String = UUID.randomUUID().toString()
    open val isDismissible: Boolean = true
    open val needPadding: Boolean = true
    open val onDismiss: (() -> Unit)? = null

    class VerificationCode(
        val userName: String,
        val state: StateFlow<VerificationCodeState>,
        val onCodeChanged: (String) -> Unit,
        val onVerify: () -> Unit,
        val onResend: () -> Unit,
        val onSuccessAnimationFinished: () -> Unit = {},
        override val onDismiss: (() -> Unit)? = null,
        override val isDismissible: Boolean = true,
        override val needPadding: Boolean = true,
        override val id: String = UUID.randomUUID().toString(),
    ) : ModalDestination()

    // Modal content and callbacks may contain authentication data.
    final override fun toString(): String =
        "ModalDestination(type=${javaClass.simpleName}, isDismissible=$isDismissible, needPadding=$needPadding)"
}
