package com.semenov.news.core.ui.interactions.data

import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.ui.interactions.data.manager.DialogManagerImpl
import com.semenov.news.core.ui.interactions.domain.model.dialog.DialogDestination
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DialogManagerImplTest {
    @Test
    fun `new collector receives the current dialog even when shown before collection`() = runTest {
        val manager = DialogManagerImpl(FakeLogger())
        assertNull(manager.currentDialog.value)
        val dialog = DialogDestination.Logout()

        manager.showDialog(dialog)

        assertSame(dialog, manager.currentDialog.first())
    }

    @Test
    fun `showing another dialog replaces the visible one for all collectors`() = runTest {
        val manager = DialogManagerImpl(FakeLogger())
        val firstDialog = DialogDestination.Logout()
        val nextDialog = DialogDestination.Logout(cancelable = false)
        val firstCollector = mutableListOf<DialogDestination?>()
        val secondCollector = mutableListOf<DialogDestination?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            manager.currentDialog.collect(firstCollector::add)
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            manager.currentDialog.collect(secondCollector::add)
        }

        manager.showDialog(firstDialog)
        manager.showDialog(nextDialog)

        assertSame(nextDialog, manager.currentDialog.value)
        assertEquals(listOf(null, firstDialog, nextDialog), firstCollector)
        assertEquals(firstCollector, secondCollector)
    }

    @Test
    fun `dismiss clears dialog and late subscribers receive empty state`() = runTest {
        val logger = FakeLogger()
        val manager = DialogManagerImpl(logger)
        val dialog = DialogDestination.Logout()
        manager.showDialog(dialog)

        manager.dismissDialog()
        manager.dismissDialog()

        assertNull(manager.currentDialog.value)
        assertNull(manager.currentDialog.first())
        assertEquals(
            listOf(
                "DialogManager: Show Dialog: $dialog",
                "DialogManager: Dismiss Dialog",
                "DialogManager: Dismiss Dialog",
            ),
            logger.messages,
        )
    }

    private class FakeLogger : AppLogger {
        val messages = mutableListOf<String>()

        override fun log(tag: String, message: String, t: Throwable?) {
            messages += "$tag: $message"
        }
    }
}
