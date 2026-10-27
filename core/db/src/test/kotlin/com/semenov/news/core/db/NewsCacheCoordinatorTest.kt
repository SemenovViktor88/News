package com.semenov.news.core.db

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NewsCacheCoordinatorTest {
    @Test
    fun `cache is unavailable before authentication and old generations remain invalid`() = runTest {
        val coordinator = NewsCacheCoordinator()
        assertNull(coordinator.currentGeneration())
        coordinator.startSession()
        val generation = checkNotNull(coordinator.currentGeneration())

        coordinator.clearForLogout {}
        coordinator.startSession()

        var wrote = false
        assertNull(coordinator.withSession(generation) { wrote = true })
        assertFalse(wrote)
    }

    @Test
    fun `logout waits for an active write before clearing and rejects later writes`() = runTest {
        val coordinator = NewsCacheCoordinator().apply { startSession() }
        val generation = checkNotNull(coordinator.currentGeneration())
        val releaseWrite = CompletableDeferred<Unit>()
        var cleared = false
        val write = launch {
            coordinator.withSession(generation) {
                releaseWrite.await()
                assertFalse(cleared)
            }
        }
        runCurrent()
        val logout = async { coordinator.clearForLogout { cleared = true } }
        runCurrent()
        assertFalse(cleared)

        releaseWrite.complete(Unit)
        write.join()
        logout.await()

        assertTrue(cleared)
        assertNull(coordinator.currentGeneration())
        assertNull(coordinator.withSession(generation) { error("Must not write") })
    }
}
