package com.semenov.news.core.ui.interactions.presentation.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.semenov.news.core.ui.designsystem.icon.NewsIcons
import com.semenov.news.core.ui.designsystem.theme.NewsTheme
import com.semenov.news.core.ui.interactions.domain.model.dialog.Action
import com.semenov.news.core.ui.interactions.domain.model.dialog.CheckboxAction
import com.semenov.news.core.ui.interactions.domain.model.dialog.DialogDestination
import com.semenov.news.core.ui.interactions.presentation.R

@Composable
fun DialogComponent(
    modifier: Modifier = Modifier,
    dialog: DialogDestination?,
    onDismissRequest: () -> Unit,
) {
    if (dialog == null) return

    val palette = NewsTheme.colors
    var actionTaken by remember(dialog) { mutableStateOf(false) }
    val dismiss = {
        if (!actionTaken) {
            actionTaken = true
            onDismissRequest()
        }
    }
    val performAction: (Action) -> Unit = { action ->
        if (!actionTaken) {
            actionTaken = true
            action.onClick()
            onDismissRequest()
        }
    }

    Dialog(
        onDismissRequest = { if (dialog.cancelable) dismiss() },
        properties = DialogProperties(
            dismissOnBackPress = dialog.cancelable,
            dismissOnClickOutside = dialog.cancelable,
            usePlatformDefaultWidth = false,
        ),
    ) {
        BoxWithConstraints(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp).widthIn(max = 420.dp),
        ) {
            Surface(
                modifier = modifier.fillMaxWidth().heightIn(max = maxHeight).testTag("app_dialog"),
                shape = RoundedCornerShape(28.dp),
                color = palette.card,
                border = BorderStroke(1.dp, palette.divider),
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    when (dialog) {
                        is DialogDestination.Logout -> {
                            Icon(
                                imageVector = NewsIcons.Logout,
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(56.dp)
                                    .background(palette.iconBackground, CircleShape)
                                    .padding(15.dp),
                            )
                            Text(
                                text = stringResource(R.string.dialog_logout_title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.text,
                                modifier = Modifier.semantics { heading() },
                            )
                            Text(
                                text = stringResource(R.string.dialog_logout_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.secondaryText,
                            )
                        }
                    }
                    dialog.checkboxAction?.let { checkboxAction ->
                        DialogCheckbox(checkboxAction, enabled = !actionTaken)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        dialog.primaryAction?.let { action ->
                            Button(
                                onClick = { performAction(action) },
                                enabled = !actionTaken,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                                    .testTag("dialog_primary_action"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.accent,
                                    contentColor = palette.checkedThumb,
                                ),
                            ) {
                                Text(stringResource(action.label), fontWeight = FontWeight.SemiBold)
                            }
                        }
                        dialog.secondaryAction?.let { action ->
                            OutlinedButton(
                                onClick = { performAction(action) },
                                enabled = !actionTaken,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                                    .testTag("dialog_secondary_action"),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, palette.divider),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.text),
                            ) {
                                Text(stringResource(action.label), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogCheckbox(action: CheckboxAction, enabled: Boolean) {
    val palette = NewsTheme.colors
    var checked by remember(action) { mutableStateOf(action.initiallyChecked) }
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .testTag("dialog_checkbox")
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = { value ->
                    checked = value
                    action.onClick(value)
                },
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = palette.accent,
                checkmarkColor = palette.checkedThumb,
                uncheckedColor = palette.secondaryText,
            ),
        )
        Text(
            text = stringResource(action.label),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.text,
        )
    }
}

@Preview(name = "Logout · light")
@Composable
private fun LogoutLightPreview() {
    NewsTheme(darkTheme = false) { LogoutPreview() }
}

@Preview(name = "Logout · dark")
@Composable
private fun LogoutDarkPreview() {
    NewsTheme(darkTheme = true) { LogoutPreview() }
}

@Composable
private fun LogoutPreview() {
    DialogComponent(
        dialog = DialogDestination.Logout(
            primaryAction = Action(R.string.dialog_yes),
            secondaryAction = Action(R.string.dialog_no),
        ),
        onDismissRequest = {},
    )
}
