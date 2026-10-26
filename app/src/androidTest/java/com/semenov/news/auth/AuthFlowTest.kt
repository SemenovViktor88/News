package com.semenov.news.auth

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.semenov.news.core.db.entity.ArticleEntity
import com.semenov.news.core.db.entity.CachedPageEntity
import com.semenov.news.core.domain.settings.AppSettings
import com.semenov.news.core.storage.domain.getBlocking
import com.semenov.news.core.storage.domain.put
import com.semenov.news.features.auth.domain.model.AuthUser
import com.semenov.news.main.MainActivity
import dagger.hilt.EntryPoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val dependencies: AuthTestDependencies
        get() = EntryPoints.get(
            InstrumentationRegistry.getInstrumentation().targetContext.applicationContext,
            AuthTestDependencies::class.java,
        )

    @Before
    fun startWithoutSavedUser() {
        runBlocking { dependencies.sessionManager().logout() }
        waitForTag("auth_username")
        composeRule.onNodeWithTag("navigation_home").assertDoesNotExist()
    }

    @After
    fun removeDemoSession() {
        runBlocking { dependencies.sessionManager().logout() }
    }

    @Test
    fun credentialsAndCodeMustBothMatchBeforeSavingTheUser() {
        captureScreen("auth-login.png")
        enterCredentials("віктор", "wrong-password")
        waitForTag("auth_error")
        composeRule.onNodeWithTag("auth_username").assertIsDisplayed()
        assertNoSavedUser()

        composeRule.onNodeWithTag("auth_password").performTextReplacement("news")
        clickScrollable("auth_sign_in")
        waitForTag("auth_code")
        assertNoSavedUser()
        captureScreen("auth-code.png")

        enterCode("0000")
        waitForTag("auth_error")
        composeRule.onNodeWithTag("auth_code").assertIsDisplayed()
        assertNoSavedUser()

        enterCode("1111")
        waitForTag("navigation_home")
        composeRule.onNodeWithTag("navigation_home").assertIsSelected()
        composeRule.onNodeWithTag("auth_code").assertDoesNotExist()
        assertEquals(AuthUser("віктор"), dependencies.sessionManager().user.value)
        assertEquals(AuthUser("віктор"), dependencies.storage().getBlocking<AuthUser?>("auth_user", null))

        openSettings()
        assertIdentity("віктор", "В")
        captureScreen("auth-profile.png")

        composeRule.activityRule.scenario.recreate()
        waitForTag("navigation_home")
        composeRule.onNodeWithTag("auth_username").assertDoesNotExist()
        openSettings()
        assertIdentity("віктор", "В")
    }

    @Test
    fun logoutClearsAllPreferencesAndDatabaseAndNewLoginStartsFresh() {
        signIn("First user")
        openSettings()
        runBlocking {
            dependencies.storage().put("auth_test_unrelated_key", "saved-value")
            dependencies.settingsStore().setDarkTheme(true)
            dependencies.settingsStore().setHapticsEnabled(false)
            val database = dependencies.database()
            database.articleDao().insertArticlesIgnoringDuplicates(
                listOf(
                    ArticleEntity(
                        contextKey = "auth-test-context",
                        url = "https://example.com/auth-test-article",
                        title = "Cached before logout",
                        description = null,
                        imageUrl = null,
                        publishedAt = null,
                        sourceName = "Test source",
                        author = null,
                        content = null,
                        page = 1,
                        position = 0,
                        cachedAt = System.currentTimeMillis(),
                    ),
                ),
            )
            database.articleDao().upsertCachedPage(
                CachedPageEntity("auth-test-context", 1, 1, System.currentTimeMillis()),
            )
            assertEquals(1, database.articleDao().articlesForPage("auth-test-context", 1).size)
            assertTrue(database.articleDao().cachedPage("auth-test-context", 1) != null)
        }
        composeRule.waitForIdle()

        clickScrollable("settings_logout")
        waitForTag("auth_username")
        assertNoSavedUser()
        assertEquals("missing", dependencies.storage().getBlocking("auth_test_unrelated_key", "missing"))
        assertEquals(AppSettings(), dependencies.settingsStore().read())
        assertAllDatabaseTablesEmpty()
        composeRule.onNodeWithTag("navigation_home").assertDoesNotExist()

        // Back from a new auth challenge must stay in auth, without restoring the old regular flow.
        enterCredentials("Anna", "news")
        waitForTag("auth_code")
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        waitForTag("auth_username")
        composeRule.onNodeWithTag("navigation_settings").assertDoesNotExist()
        assertNoSavedUser()

        composeRule.activityRule.scenario.recreate()
        waitForTag("auth_username")
        composeRule.onNodeWithTag("navigation_home").assertDoesNotExist()
        signIn("Anna")
        composeRule.onNodeWithTag("navigation_home").assertIsSelected()
        openSettings()
        assertIdentity("Anna", "A")
    }

    private fun signIn(name: String) {
        enterCredentials(name, "news")
        waitForTag("auth_code")
        enterCode("1111")
        waitForTag("navigation_home")
    }

    private fun enterCredentials(name: String, password: String) {
        composeRule.onNodeWithTag("auth_username").performTextReplacement(name)
        composeRule.onNodeWithTag("auth_password").performTextReplacement(password)
        clickScrollable("auth_sign_in")
    }

    private fun enterCode(code: String) {
        composeRule.onNodeWithTag("auth_code").performTextReplacement(code)
        clickScrollable("auth_verify")
    }

    private fun openSettings() {
        composeRule.onNodeWithTag("navigation_settings").performClick()
        waitForTag("settings_user_name")
        composeRule.onNodeWithTag("settings_screen").assertIsDisplayed()
    }

    private fun assertIdentity(name: String, initial: String) {
        composeRule.onNodeWithTag("settings_user_name").assertTextEquals(name)
        composeRule.onNodeWithTag("settings_user_initial").assertTextEquals(initial)
    }

    private fun clickScrollable(tag: String) {
        composeRule.onNodeWithTag(tag).performScrollTo().performClick()
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
    }

    private fun assertNoSavedUser() {
        assertNull(dependencies.sessionManager().user.value)
        assertNull(dependencies.storage().getBlocking<AuthUser?>("auth_user", null))
    }

    private fun assertAllDatabaseTablesEmpty() = runBlocking {
        withContext(Dispatchers.IO) {
            val sqlDatabase = dependencies.database().openHelper.readableDatabase
            val tableNames = sqlDatabase.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' " +
                    "AND name NOT LIKE 'sqlite_%' AND name NOT IN ('android_metadata', 'room_master_table')",
            ).use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
            }
            assertTrue(tableNames.containsAll(listOf("articles", "cached_pages")))
            tableNames.forEach { table ->
                val quotedTable = table.replace("\"", "\"\"")
                sqlDatabase.query("SELECT COUNT(*) FROM \"$quotedTable\"").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("Logout must clear $table", 0L, cursor.getLong(0))
                }
            }
        }
    }

    private fun captureScreen(filename: String) {
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.openFileOutput(filename, Context.MODE_PRIVATE).use { output ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
    }
}
