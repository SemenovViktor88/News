package com.semenov.news.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.semenov.news.core.ui.navigation.presentation.NavigationManagerImpl
import com.semenov.news.main.components.MainContainer
import com.semenov.news.ui.theme.NewsTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel by viewModels<MainViewModel>()

    @Inject
    lateinit var navigationManager: NavigationManagerImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NewsTheme {
                MainContainer(
                    viewModel = mainViewModel,
                    navigationManager = navigationManager,
                )
            }
        }
    }
}
