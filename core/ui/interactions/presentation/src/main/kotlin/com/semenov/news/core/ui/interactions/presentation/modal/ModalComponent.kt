package com.semenov.news.core.ui.interactions.presentation.modal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.semenov.news.core.ui.designsystem.theme.NewsTheme
import com.semenov.news.core.ui.interactions.domain.model.modal.ModalDestination

/** Keeps outgoing content composed until the bottom sheet finishes its exit animation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalComponent(
    modal: ModalDestination?,
    requestDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var displayedModal by remember { mutableStateOf<ModalDestination?>(null) }
    val currentModal = modal ?: displayedModal
    val busy = when (currentModal) {
        is ModalDestination.VerificationCode -> {
            val state by currentModal.state.collectAsStateWithLifecycle()
            state.isSubmitting || state.isVerified
        }
        null -> false
    }
    val canDismiss = currentModal?.isDismissible == true && !busy
    val latestCanHide by rememberUpdatedState(modal == null || canDismiss)
    val latestModal by rememberUpdatedState(modal)
    val latestDismiss by rememberUpdatedState(requestDismiss)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || latestCanHide },
    )
    var dismissedId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(modal?.id) {
        if (modal != null) {
            displayedModal = modal
            dismissedId = null
        } else {
            sheetState.hide()
            displayedModal = null
        }
    }
    LaunchedEffect(displayedModal?.id, modal?.id) {
        if (modal != null && displayedModal?.id == modal.id) sheetState.show()
    }

    currentModal?.let { destination ->
        val dismiss = {
            // A finishing animation from a replaced sheet must not dismiss its replacement.
            val busyNow = destination.state.value.let {
                it.isSubmitting || it.isVerified
            }
            if (destination.isDismissible && !busyNow && latestModal?.id == destination.id && dismissedId != destination.id) {
                dismissedId = destination.id
                latestDismiss()
                destination.onDismiss?.invoke()
            }
        }
        ModalBottomSheet(
            onDismissRequest = dismiss,
            modifier = modifier.fillMaxWidth().testTag("auth_modal"),
            sheetState = sheetState,
            sheetMaxWidth = 480.dp,
            sheetGesturesEnabled = canDismiss,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            containerColor = NewsTheme.colors.card,
            contentColor = NewsTheme.colors.text,
            scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.48f),
            // The sheet handles IME insets; omitting the top inset keeps it stable during resizing.
            contentWindowInsets = {
                WindowInsets(top = 0, bottom = WindowInsets.systemBars.getBottom(LocalDensity.current))
            },
            properties = ModalBottomSheetProperties(
                shouldDismissOnBackPress = canDismiss,
                shouldDismissOnClickOutside = canDismiss,
            ),
            dragHandle = {
                Box(
                    Modifier.padding(top = 12.dp, bottom = 4.dp)
                        .width(38.dp).height(4.dp)
                        .background(NewsTheme.colors.secondaryText.copy(alpha = 0.3f), RoundedCornerShape(2.dp)),
                )
            },
        ) {
            key(destination.id) {
                VerificationCodeContent(
                    modal = destination,
                    isActive = modal?.id == destination.id,
                    onDismiss = dismiss,
                    canDismiss = canDismiss,
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = if (destination.needPadding) 24.dp else 0.dp),
                )
            }
        }
    }
}
