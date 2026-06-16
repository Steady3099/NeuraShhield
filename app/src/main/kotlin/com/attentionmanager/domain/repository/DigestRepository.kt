package com.attentionmanager.domain.repository

import com.attentionmanager.data.database.DigestEntity
import kotlinx.coroutines.flow.Flow

interface DigestRepository {
    fun observeDigests(): Flow<List<DigestEntity>>
    fun observeDigestCount(): Flow<Int>
    suspend fun insert(digest: DigestEntity): Long
}
