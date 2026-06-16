package com.attentionmanager.domain.repository

import com.attentionmanager.data.database.SpamLogEntity
import kotlinx.coroutines.flow.Flow

interface SpamLogRepository {
    fun observeSpamLog(): Flow<List<SpamLogEntity>>
    suspend fun insert(entity: SpamLogEntity)
    suspend fun clearAll()
}
