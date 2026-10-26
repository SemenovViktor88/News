package com.semenov.news.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
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
) {
    val palette = NewsTheme.colors
    val focusManager = LocalFocusManager.current
    val isLogin = state.stage == AuthStage.Login
    val submit = {
        focusManager.clearFocus()
        onIntent(if (isLogin) AuthIntent.SignIn else AuthIntent.VerifyCode)
    }
    Box(
        modifier = modifier.fillMaxSize().background(palette.background).safeDrawingPadding().imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
//            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
//                Box(
//                    modifier = Modifier.size(48.dp).background(palette.avatarBackground, CircleShape),
//                    contentAlignment = Alignment.Center,
//                ) {
//                    Text("N", color = palette.avatarText, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
//                }
//                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
//                    Text(stringResource(R.string.auth_app_name), color = palette.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
//                    Text(stringResource(R.string.auth_tagline), color = palette.secondaryText, fontSize = 13.sp)
//                }
//            }
//            Spacer(Modifier.height(36.dp))
//            Surface(color = palette.iconBackground, shape = RoundedCornerShape(50)) {
//                Text(
//                    stringResource(if (isLogin) R.string.auth_step_login else R.string.auth_step_code),
//                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
//                    color = palette.accent,
//                    fontSize = 11.sp,
//                    fontWeight = FontWeight.SemiBold,
//                    letterSpacing = 1.sp,
//                )
//            }
//            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(if (isLogin) R.string.auth_welcome else R.string.auth_verify_title),
                color = palette.text,
                fontSize = 36.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (isLogin) stringResource(R.string.auth_login_description)
                else stringResource(R.string.auth_code_description, state.userName),
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
                    if (isLogin) {
                        LoginFields(state, onIntent, submit)
                    } else {
                        CodeField(state, onIntent, submit)
                    }
                    state.error?.let { error ->
                        Text(
                            text = error.message(),
                            modifier = Modifier.testTag("auth_error").semantics { liveRegion = LiveRegionMode.Polite },
                            color = palette.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Button(
                        onClick = submit,
                        enabled = !state.isSubmitting &&
                            if (isLogin) state.userName.isNotBlank() && state.password.isNotEmpty() else state.code.length == 4,
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                            .testTag(if (isLogin) "auth_sign_in" else "auth_verify"),
                        shape = RoundedCornerShape(17.dp),
                    ) {
                        if (state.isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = palette.checkedThumb)
                        } else {
                            Text(
                                stringResource(if (isLogin) R.string.auth_continue else R.string.auth_verify_button),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    if (!isLogin) {
                        TextButton(
                            onClick = {
                                focusManager.clearFocus()
                                onIntent(AuthIntent.BackToLogin)
                            },
                            enabled = !state.isSubmitting,
                            modifier = Modifier.fillMaxWidth().testTag("auth_back"),
                        ) { Text(stringResource(R.string.auth_change_user)) }
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
                stringResource(if (isLogin) R.string.auth_demo_login else R.string.auth_demo_code),
                modifier = Modifier.testTag("auth_demo_hint"),
                color = palette.secondaryText,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
        }
    }
}

@Composable
private fun LoginFields(state: AuthState, onIntent: (AuthIntent) -> Unit, onSubmit: () -> Unit) {
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
        enabled = !state.isSubmitting,
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
            TextButton(onClick = { passwordVisible = !passwordVisible }, enabled = !state.isSubmitting) {
                Text(stringResource(if (passwordVisible) R.string.auth_hide_password else R.string.auth_show_password), fontSize = 12.sp)
            }
        },
        modifier = Modifier.fillMaxWidth().testTag("auth_password"),
        enabled = !state.isSubmitting,
        isError = state.error == AuthError.InvalidCredentials,
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = authFieldColors(),
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (!state.isSubmitting) onSubmit() }),
    )
}

@Composable
private fun CodeField(state: AuthState, onIntent: (AuthIntent) -> Unit, onSubmit: () -> Unit) {
    OutlinedTextField(
        value = state.code,
        onValueChange = { onIntent(AuthIntent.CodeChanged(it)) },
        label = { Text(stringResource(R.string.auth_code_label)) },
        modifier = Modifier.fillMaxWidth().testTag("auth_code"),
        enabled = !state.isSubmitting,
        isError = state.error == AuthError.InvalidCode,
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = authFieldColors(),
        textStyle = MaterialTheme.typography.headlineMedium.copy(
            textAlign = TextAlign.Center,
            letterSpacing = 14.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (state.code.length == 4 && !state.isSubmitting) onSubmit() }),
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
        AuthError.Unavailable -> R.string.auth_error_unavailable
    },
)
