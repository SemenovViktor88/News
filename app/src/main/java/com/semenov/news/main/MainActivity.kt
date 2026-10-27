package com.semenov.news.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.semenov.news.core.ui.navigation.presentation.NavigationManagerImpl
import com.semenov.news.main.components.MainContainer
import com.semenov.news.core.ui.designsystem.theme.NewsTheme
import com.semenov.news.core.ui.interactions.domain.manager.HapticEffectManager
import com.semenov.news.core.ui.interactions.presentation.haptic.HapticEffectHandler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel by viewModels<MainViewModel>()

    @Inject
    lateinit var navigationManager: NavigationManagerImpl

    @Inject
    lateinit var hapticEffectManager: HapticEffectManager

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setOnExitAnimationListener { splash ->
            splash.view.animate()
                .alpha(0f)
                .setDuration(180L)
                .withEndAction(splash::remove)
                .start()
        }
        enableEdgeToEdge()
        setContent {
            val state by mainViewModel.state.collectAsStateWithLifecycle()
            val darkTheme = state.darkTheme ?: isSystemInDarkTheme()
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }

            NewsTheme(darkTheme = darkTheme) {
                HapticEffectHandler(hapticEffects = hapticEffectManager.effects)
                MainContainer(
                    viewModel = mainViewModel,
                    navigationManager = navigationManager,
                )
            }
        }
    }
}
