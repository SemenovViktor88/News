package com.semenov.news.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semenov.news.core.ui.designsystem.theme.NewsTheme
import com.semenov.news.screens.auth.model.AuthError
import com.semenov.news.screens.auth.model.AuthIntent
import com.semenov.news.screens.auth.model.AuthStage
import com.semenov.news.screens.auth.model.AuthState

@Composable
fun AuthContent(
    state: AuthState,
    onIntent: (AuthIntent) -> Unit,
    modifier: Modifier = Modifier,
    isPermissionRequestPending: Boolean = false,
    onOpenNotificationSettings: () -> Unit = {},
) {
    val palette = NewsTheme.colors
    val focusManager = LocalFocusManager.current
    val enabled = state.stage == AuthStage.Login && !state.isSubmitting && !isPermissionRequestPending
    val submit = {
        if (enabled && state.userName.isNotBlank() && state.password.isNotEmpty()) {
            focusManager.clearFocus()
            onIntent(AuthIntent.SignIn)
        }
    }
    Box(
        modifier = modifier.fillMaxSize().background(palette.background).safeDrawingPadding().imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Text(
                stringResource(R.string.auth_welcome),
                color = palette.text,
                fontSize = 36.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.auth_login_description),
                color = palette.secondaryText,
                fontSize = 15.sp,
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(24.dp))
            Surface(color = palette.card, shape = RoundedCornerShape(28.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    LoginFields(state, enabled, onIntent, submit)
                    if (state.stage == AuthStage.Login) {
                        state.error?.let { error ->
                            Text(
                                text = error.message(),
                                modifier = Modifier.testTag("auth_error").semantics { liveRegion = LiveRegionMode.Polite },
                                color = palette.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (error == AuthError.NotificationsUnavailable) {
                                TextButton(
                                    onClick = onOpenNotificationSettings,
                                    modifier = Modifier.testTag("auth_notification_settings"),
                                ) {
                                    Text(stringResource(R.string.auth_notification_settings))
                                }
                            }
                        }
                    }
                    Button(
                        onClick = submit,
                        enabled = enabled && state.userName.isNotBlank() && state.password.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("auth_sign_in"),
                        shape = RoundedCornerShape(17.dp),
                    ) {
                        if (state.isSubmitting && state.stage == AuthStage.Login) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = palette.checkedThumb)
                        } else {
                            Text(stringResource(R.string.auth_continue), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.auth_demo_title),
                color = palette.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                stringResource(R.string.auth_demo_login),
                modifier = Modifier.testTag("auth_demo_hint"),
                color = palette.secondaryText,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
        }
    }
}

@Composable
private fun LoginFields(state: AuthState, enabled: Boolean, onIntent: (AuthIntent) -> Unit, onSubmit: () -> Unit) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = state.userName,
        onValueChange = { onIntent(AuthIntent.UserNameChanged(it)) },
        label = { Text(stringResource(R.string.auth_user_name)) },
        supportingText = {
            Text(stringResource(R.string.auth_name_length, state.userName.codePointCount(0, state.userName.length)))
        },
        modifier = Modifier.fillMaxWidth().testTag("auth_username"),
        enabled = enabled,
        isError = state.error == AuthError.InvalidUserName,
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = authFieldColors(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
    )
    OutlinedTextField(
        value = state.password,
        onValueChange = { onIntent(AuthIntent.PasswordChanged(it)) },
        label = { Text(stringResource(R.string.auth_password)) },
        trailingIcon = {
            TextButton(onClick = { passwordVisible = !passwordVisible }, enabled = enabled) {
                Text(stringResource(if (passwordVisible) R.string.auth_hide_password else R.string.auth_show_password), fontSize = 12.sp)
            }
        },
        modifier = Modifier.fillMaxWidth().testTag("auth_password"),
        enabled = enabled,
        isError = state.error == AuthError.InvalidCredentials,
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = authFieldColors(),
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (enabled) onSubmit() }),
    )
}

@Composable
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NewsTheme.colors.accent,
    unfocusedBorderColor = NewsTheme.colors.divider,
    focusedLabelColor = NewsTheme.colors.accent,
    unfocusedLabelColor = NewsTheme.colors.secondaryText,
)

@Composable
private fun AuthError.message(): String = stringResource(
    when (this) {
        AuthError.InvalidUserName -> R.string.auth_error_user_name
        AuthError.InvalidCredentials -> R.string.auth_error_credentials
        AuthError.InvalidCode -> R.string.auth_error_code
        AuthError.NotificationsUnavailable -> R.string.auth_error_notifications
        AuthError.Unavailable -> R.string.auth_error_unavailable
    },
)
