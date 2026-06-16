package com.attentionmanager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attentionmanager.data.database.ContactPriorityEntity
import com.attentionmanager.data.database.SpamLogEntity
import com.attentionmanager.domain.repository.ContactPriorityRepository
import com.attentionmanager.domain.repository.PreferenceRepository
import com.attentionmanager.domain.repository.SpamLogRepository
import com.attentionmanager.domain.usecase.DigestScheduler
import com.attentionmanager.domain.usecase.UserFeedbackUseCase
import com.attentionmanager.ml.PriorityRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferenceRepository: PreferenceRepository,
    contactPriorityRepository: ContactPriorityRepository,
    spamLogRepository: SpamLogRepository,
    private val digestScheduler: DigestScheduler,
    private val userFeedbackUseCase: UserFeedbackUseCase
) : ViewModel() {
    private val rulesText = MutableStateFlow(defaultRulesText())

    val uiState = combine(
        preferenceRepository.preferences,
        contactPriorityRepository.observeContactPriorities(),
        spamLogRepository.observeSpamLog(),
        rulesText
    ) { preferences, contacts, spamLog, rules ->
        SettingsUiState(
            filterEnabled = preferences.aiFilterEnabled,
            digestIntervalHours = preferences.digestIntervalHours,
            digestIntervalText = preferences.digestIntervalHours.toString(),
            contacts = contacts,
            spamLog = spamLog.take(20),
            priorityRulesText = rules
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setFilterEnabled(enabled: Boolean) {
        viewModelScope.launch { preferenceRepository.setFilterEnabled(enabled) }
    }

    fun updateRulesText(value: String) {
        rulesText.value = value
    }

    fun saveDigestInterval(text: String) {
        val hours = text.trim().toIntOrNull()?.coerceIn(1, 24) ?: 1
        viewModelScope.launch {
            preferenceRepository.setDigestIntervalHours(hours)
            digestScheduler.syncDigestSchedule(hours)
        }
    }

    fun resetAiModel() {
        viewModelScope.launch {
            userFeedbackUseCase.resetAiModel()
            rulesText.value = defaultRulesText()
        }
    }

    private fun defaultRulesText(): String =
        buildString {
            appendLine("URGENT")
            PriorityRules.urgentPatterns.forEach { appendLine(it.name) }
            appendLine()
            appendLine("LOW")
            PriorityRules.lowPatterns.forEach { appendLine(it.name) }
        }
}

data class SettingsUiState(
    val filterEnabled: Boolean = true,
    val digestIntervalHours: Int = 1,
    val digestIntervalText: String = "1",
    val contacts: List<ContactPriorityEntity> = emptyList(),
    val spamLog: List<SpamLogEntity> = emptyList(),
    val priorityRulesText: String = ""
)
