package com.attentionmanager.ui.digest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attentionmanager.data.database.DigestEntity
import com.attentionmanager.domain.repository.DigestRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DigestViewModel(
    digestRepository: DigestRepository
) : ViewModel() {
    val digests: StateFlow<List<DigestEntity>> = digestRepository.observeDigests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
