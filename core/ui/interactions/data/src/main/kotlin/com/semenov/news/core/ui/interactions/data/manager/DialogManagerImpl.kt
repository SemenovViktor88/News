package com.semenov.news.core.ui.interactions.data.manager

import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.ui.interactions.domain.manager.DialogManager
import com.semenov.news.core.ui.interactions.domain.model.dialog.DialogDestination
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class DialogManagerImpl @Inject constructor(
    private val logger: AppLogger,
) : DialogManager {
    private val mutableCurrentDialog = MutableStateFlow<DialogDestination?>(null)
    override val currentDialog = mutableCurrentDialog.asStateFlow()

    override suspend fun showDialog(dialog: DialogDestination) {
        logger.log(tag = "DialogManager", message = "Show Dialog: $dialog")
        mutableCurrentDialog.emit(dialog)
    }

    override suspend fun dismissDialog() {
        logger.log(tag = "DialogManager", message = "Dismiss Dialog")
        mutableCurrentDialog.emit(null)
    }
}
