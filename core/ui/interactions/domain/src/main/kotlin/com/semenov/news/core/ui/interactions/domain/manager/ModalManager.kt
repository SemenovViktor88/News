package com.semenov.news.core.ui.interactions.domain.manager

import com.semenov.news.core.ui.interactions.domain.model.modal.ModalDestination
import kotlinx.coroutines.flow.StateFlow

interface ModalManager {
    val currentModal: StateFlow<ModalDestination?>

    suspend fun showModal(modal: ModalDestination)

    suspend fun dismissModal()
}
