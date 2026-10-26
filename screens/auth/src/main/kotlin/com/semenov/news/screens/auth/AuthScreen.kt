package com.semenov.news.screens.auth

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.semenov.news.core.ui.mvi.presentation.BaseScreen
import com.semenov.news.screens.auth.model.AuthIntent
import com.semenov.news.screens.auth.model.AuthStage

@Composable
fun AuthScreen(viewModel: AuthViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(enabled = state.stage == AuthStage.Code) {
        viewModel.processIntent(AuthIntent.BackToLogin)
    }
    BaseScreen(state = state, init = viewModel::initialize) {
        AuthContent(state = state, onIntent = viewModel::processIntent)
    }
}
