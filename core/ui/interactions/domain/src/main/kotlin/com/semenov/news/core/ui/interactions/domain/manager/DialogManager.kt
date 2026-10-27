package com.semenov.news.core.ui.interactions.domain.manager

import com.semenov.news.core.ui.interactions.domain.model.dialog.DialogDestination
import kotlinx.coroutines.flow.StateFlow

interface DialogManager {
    val currentDialog: StateFlow<DialogDestination?>

    suspend fun showDialog(dialog: DialogDestination)

    suspend fun dismissDialog()
}
