package com.attentionmanager.data.repository

import com.attentionmanager.data.database.SpamLogDao
import com.attentionmanager.data.database.SpamLogEntity
import com.attentionmanager.domain.repository.SpamLogRepository
import kotlinx.coroutines.flow.Flow

class SpamLogRepositoryImpl(
    private val dao: SpamLogDao
) : SpamLogRepository {
    override fun observeSpamLog(): Flow<List<SpamLogEntity>> = dao.observeSpamLog()

    override suspend fun insert(entity: SpamLogEntity) = dao.insert(entity)

    override suspend fun clearAll() = dao.clearAll()
}
