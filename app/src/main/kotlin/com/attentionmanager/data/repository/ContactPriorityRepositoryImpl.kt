package com.attentionmanager.data.repository

import com.attentionmanager.data.database.ContactPriorityDao
import com.attentionmanager.data.database.ContactPriorityEntity
import com.attentionmanager.domain.repository.ContactPriorityRepository
import kotlinx.coroutines.flow.Flow

class ContactPriorityRepositoryImpl(
    private val dao: ContactPriorityDao
) : ContactPriorityRepository {
    override fun observeContactPriorities(): Flow<List<ContactPriorityEntity>> =
        dao.observeContactPriorities()

    override suspend fun boostFor(sender: String?, packageName: String): Float {
        val normalized = sender?.trim().takeUnless { it.isNullOrBlank() } ?: return 0f
        return dao.findBySender(normalized, packageName)?.clampedPriorityBoost ?: 0f
    }

    override suspend fun upsert(entity: ContactPriorityEntity) =
        dao.upsert(entity.copy(priorityBoost = entity.priorityBoost.coerceIn(-1f, 1f)))

    override suspend fun clearAll() = dao.clearAll()
}
