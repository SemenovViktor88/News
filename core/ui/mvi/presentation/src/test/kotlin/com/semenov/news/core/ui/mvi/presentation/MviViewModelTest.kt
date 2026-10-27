package com.semenov.news.core.ui.mvi.presentation

import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.ui.mvi.domain.model.ErrorState
import com.semenov.news.core.ui.mvi.domain.model.UiEffect
import com.semenov.news.core.ui.mvi.domain.model.UiIntent
import com.semenov.news.core.ui.mvi.domain.model.UiPartial
import com.semenov.news.core.ui.mvi.domain.model.UiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MviViewModelTest {
    @Test
    fun `failed source is reported without stopping another binding`() = runTest {
        val logger = RecordingLogger()
        val viewModel = TestViewModel(backgroundScope, logger)
        val sibling = MutableStateFlow(0)
        val received = mutableListOf<Int>()
        viewModel.bindFlow(sibling, received::add)
        val failure = IllegalStateException("Subscription failed")
        val failedJob = viewModel.bindFlow(flow<Int> { throw failure }) {}
        runCurrent()

        sibling.value = 1
        runCurrent()

        assertTrue(failedJob.isCompleted)
        assertFalse(failedJob.isCancelled)
        assertEquals(listOf(0, 1), received)
        assertEquals(listOf(failure), viewModel.errors)
        assertEquals(1, logger.errors.size)
        assertSame(failure, logger.errors.single().throwable)
        assertTrue(logger.errors.single().message.contains("bind function"))
    }

    @Test
    fun `failed binding action is reported and stops processing its source`() = runTest {
        val logger = RecordingLogger()
        val viewModel = TestViewModel(backgroundScope, logger)
        val failure = IllegalArgumentException("Cannot reduce value")
        val received = mutableListOf<Int>()
        val source = flow {
            emit(1)
            emit(2)
            emit(3)
        }
        val job = viewModel.bindFlow(source) { value ->
            if (value == 2) throw failure
            received.add(value)
        }
        runCurrent()

        assertTrue(job.isCompleted)
        assertEquals(listOf(1), received)
        assertEquals(listOf(failure), viewModel.errors)
        assertEquals(1, logger.errors.size)
        assertSame(failure, logger.errors.single().throwable)
        assertTrue(logger.errors.single().message.contains("bind function"))
    }

    @Test
    fun `scope cancellation releases bindings without reporting an error`() = runTest {
        val scopeJob = SupervisorJob()
        val bindingScope = CoroutineScope(scopeJob + StandardTestDispatcher(testScheduler))
        val logger = RecordingLogger()
        val viewModel = TestViewModel(bindingScope, logger)
        var released = false
        val job = viewModel.bindFlow(flow<Int> {
            try {
                awaitCancellation()
            } finally {
                released = true
            }
        }) {}
        runCurrent()

        scopeJob.cancel()
        runCurrent()

        assertTrue(job.isCancelled)
        assertTrue(released)
        assertTrue(viewModel.errors.isEmpty())
        assertTrue(logger.errors.isEmpty())
    }

    private class TestViewModel(
        override val scope: CoroutineScope,
        logger: AppLogger,
    ) : MviViewModel<TestState, TestPartial, TestIntent, TestEffect>(TestState, logger) {
        val errors = mutableListOf<Throwable>()

        fun <T> bindFlow(source: Flow<T>, action: suspend (T) -> Unit) = source.bind(action)

        override suspend fun processInit() = Unit

        override suspend fun handleIntent(intent: TestIntent) = Unit

        override suspend fun reduceWithPartial(partial: TestPartial, old: TestState) = old

        override suspend fun handleError(error: Throwable, intent: TestIntent?) {
            errors.add(error)
        }
    }

    private object TestState : UiState {
        override val hasContent = true
        override val initialErrorState: ErrorState? = null
    }

    private interface TestPartial : UiPartial
    private interface TestIntent : UiIntent
    private interface TestEffect : UiEffect

    private class RecordingLogger : AppLogger {
        data class Error(val message: String, val throwable: Throwable)

        val errors = mutableListOf<Error>()

        override fun log(tag: String, message: String, t: Throwable?) {
            if (t != null) errors.add(Error(message, t))
        }
    }
}
