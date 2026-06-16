package com.attentionmanager.domain.repository

import com.attentionmanager.data.database.ContactPriorityEntity
import kotlinx.coroutines.flow.Flow

interface ContactPriorityRepository {
    fun observeContactPriorities(): Flow<List<ContactPriorityEntity>>
    suspend fun boostFor(sender: String?, packageName: String): Float
    suspend fun upsert(entity: ContactPriorityEntity)
    suspend fun clearAll()
}
