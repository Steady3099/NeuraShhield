package com.attentionmanager.domain.usecase

import com.attentionmanager.data.database.DigestEntity
import com.attentionmanager.domain.model.AttentionError
import com.attentionmanager.domain.model.AttentionResult
import com.attentionmanager.domain.model.DigestItem
import com.attentionmanager.domain.model.DigestSummary
import com.attentionmanager.domain.repository.DigestRepository
import com.attentionmanager.domain.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DigestGeneratorUseCase(
    private val notificationRepository: NotificationRepository,
    private val digestRepository: DigestRepository,
    private val categoryResolver: AppCategoryResolver,
    private val formatter: DigestSummaryFormatter = DigestSummaryFormatter(),
    private val publisher: DigestPublisher
) {
    suspend fun generateAndPostDigest(): AttentionResult<DigestSummary> = withContext(Dispatchers.Default) {
        try {
            val candidates = notificationRepository.digestCandidates()
            val items = candidates.map {
                DigestItem(
                    notificationId = it.id,
                    packageName = it.packageName,
                    appLabel = categoryResolver.labelFor(it.packageName),
                    category = categoryResolver.categoryFor(it.packageName, it.title, it.body),
                    title = it.title,
                    body = it.body,
                    priorityTier = it.priorityTier
                )
            }
            val summary = formatter.format(items)
            if (summary.notificationIds.isNotEmpty()) {
                digestRepository.insert(
                    DigestEntity(
                        generatedAt = System.currentTimeMillis(),
                        summaryText = summary.detailText,
                        notificationIds = summary.notificationIds
                    )
                )
                notificationRepository.markDigested(summary.notificationIds)
                publisher.postDigest(summary)
            }
            AttentionResult.Success(summary)
        } catch (throwable: Throwable) {
            AttentionResult.Failure(AttentionError.Unknown("Unable to generate digest.", throwable))
        }
    }
}

interface DigestPublisher {
    fun postDigest(summary: DigestSummary)
}
