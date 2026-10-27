package com.semenov.news.core.ui.interactions.data.manager

import com.semenov.news.core.domain.AppLogger
import com.semenov.news.core.ui.interactions.domain.manager.ModalManager
import com.semenov.news.core.ui.interactions.domain.model.modal.ModalDestination
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class ModalManagerImpl @Inject constructor(
    private val logger: AppLogger,
) : ModalManager {
    private val mutableCurrentModal = MutableStateFlow<ModalDestination?>(null)
    override val currentModal = mutableCurrentModal.asStateFlow()

    override suspend fun showModal(modal: ModalDestination) {
        logger.log(tag = "ModalManager", message = "Show Modal: $modal")
        mutableCurrentModal.emit(modal)
    }

    override suspend fun dismissModal() {
        logger.log(tag = "ModalManager", message = "Dismiss Modal")
        mutableCurrentModal.emit(null)
    }
}
