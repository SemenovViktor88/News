package com.semenov.news.core.ui.interactions.data

import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.ui.interactions.data.manager.ModalManagerImpl
import com.semenov.news.core.ui.interactions.domain.model.modal.ModalDestination
import com.semenov.news.core.ui.interactions.domain.model.modal.VerificationCodeState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ModalManagerImplTest {
    @Test
    fun `late collector receives the currently shown modal`() = runTest {
        val manager = ModalManagerImpl(FakeLogger())
        assertNull(manager.currentModal.value)
        val modal = verificationModal()

        manager.showModal(modal)

        assertSame(modal, manager.currentModal.first())
    }

    @Test
    fun `showing another modal replaces the current modal for every collector`() = runTest {
        val manager = ModalManagerImpl(FakeLogger())
        val firstModal = verificationModal()
        val replacement = verificationModal()
        val firstCollector = mutableListOf<ModalDestination?>()
        val secondCollector = mutableListOf<ModalDestination?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            manager.currentModal.collect(firstCollector::add)
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            manager.currentModal.collect(secondCollector::add)
        }

        manager.showModal(firstModal)
        manager.showModal(replacement)

        assertSame(replacement, manager.currentModal.value)
        assertEquals(listOf(null, firstModal, replacement), firstCollector)
        assertEquals(firstCollector, secondCollector)
    }

    @Test
    fun `dismiss clears modal without invoking the user dismissal callback`() = runTest {
        val logger = FakeLogger()
        val manager = ModalManagerImpl(logger)
        var dismissCalls = 0
        val modal = verificationModal(onDismiss = { dismissCalls++ })
        manager.showModal(modal)

        manager.dismissModal()
        manager.dismissModal()

        assertNull(manager.currentModal.value)
        assertNull(manager.currentModal.first())
        assertEquals(0, dismissCalls)
        assertEquals(
            listOf(
                "ModalManager: Show Modal: $modal",
                "ModalManager: Dismiss Modal",
                "ModalManager: Dismiss Modal",
            ),
            logger.messages,
        )
    }

    @Test
    fun `each modal gets an independent stable identity`() {
        val state = MutableStateFlow(VerificationCodeState())
        val firstModal = verificationModal(state = state)
        val secondModal = verificationModal()

        assertNotEquals(firstModal.id, secondModal.id)
        val firstId = firstModal.id
        state.value = VerificationCodeState(code = "1234")
        assertEquals(firstId, firstModal.id)
    }

    @Test
    fun `modal state and manager logs exclude authentication details and callback captures`() = runTest {
        val logger = FakeLogger()
        val manager = ModalManagerImpl(logger)
        val userName = "Private user name"
        val code = "9876"
        val callbackSecret = "Private callback data"
        val state = VerificationCodeState(code = code)
        val callback = object : () -> Unit {
            override fun invoke() = Unit
            override fun toString(): String = callbackSecret
        }
        val modal = ModalDestination.VerificationCode(
            userName = userName,
            state = MutableStateFlow(state),
            onCodeChanged = {},
            onVerify = callback,
            onResend = callback,
            onDismiss = callback,
            id = "private-modal-id",
        )

        manager.showModal(modal)
        manager.dismissModal()

        val text = listOf(state.toString(), modal.toString()) + logger.messages
        text.forEach { output ->
            assertFalse(output.contains(userName))
            assertFalse(output.contains(code))
            assertFalse(output.contains(callbackSecret))
            assertFalse(output.contains(modal.id))
        }
    }

    private fun verificationModal(
        onDismiss: (() -> Unit)? = null,
        state: MutableStateFlow<VerificationCodeState> = MutableStateFlow(VerificationCodeState()),
    ) = ModalDestination.VerificationCode(
        userName = "Reader",
        state = state,
        onCodeChanged = {},
        onVerify = {},
        onResend = {},
        onDismiss = onDismiss,
    )

    private class FakeLogger : AppLogger {
        val messages = mutableListOf<String>()

        override fun log(tag: String, message: String, t: Throwable?) {
            messages += "$tag: $message"
        }
    }
}
