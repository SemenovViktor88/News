package com.semenov.news.core.ui.navigation.presentation

import com.semenov.news.core.ui.navigation.domain.AppDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class NavigationManagerImplTest {
    @Test
    fun `home is the active start flow and each flow has its own root back stack`() {
        val manager = NavigationManagerImpl()

        assertEquals(AppDestination.Home, manager.activeDestination.value)
        assertEquals(listOf(NavigationKey.Home), manager.homeBackStack)
        assertEquals(listOf(NavigationKey.Categories), manager.categoriesBackStack)
    }

    @Test
    fun `navigate switches the active flow without combining back stacks`() {
        val manager = NavigationManagerImpl()

        manager.navigate(AppDestination.Categories)

        assertEquals(AppDestination.Categories, manager.activeDestination.value)
        assertSame(manager.categoriesBackStack, manager.activeBackStack)
        assertEquals(listOf(NavigationKey.Home), manager.homeBackStack)
        assertEquals(listOf(NavigationKey.Categories), manager.categoriesBackStack)
    }

    @Test
    fun `switching back restores the same home back stack`() {
        val manager = NavigationManagerImpl()
        val originalHomeBackStack = manager.homeBackStack
        manager.navigate(AppDestination.Categories)

        manager.navigate(AppDestination.Home)

        assertSame(originalHomeBackStack, manager.activeBackStack)
        assertEquals(listOf(NavigationKey.Home), manager.activeBackStack)
    }

    @Test
    fun `go back pops the active flow before returning to home`() {
        val manager = NavigationManagerImpl()
        manager.homeBackStack.add(NavigationKey.Home)
        manager.navigate(AppDestination.Categories)
        manager.categoriesBackStack.add(NavigationKey.Categories)

        manager.goBack()

        assertEquals(listOf(NavigationKey.Home, NavigationKey.Home), manager.homeBackStack)
        assertEquals(listOf(NavigationKey.Categories), manager.categoriesBackStack)
        assertEquals(AppDestination.Categories, manager.activeDestination.value)

        manager.goBack()

        assertEquals(listOf(NavigationKey.Categories), manager.categoriesBackStack)
        assertEquals(AppDestination.Home, manager.activeDestination.value)
    }
}
