package com.attentionmanager.data.repository

import com.attentionmanager.data.database.DigestDao
import com.attentionmanager.data.database.DigestEntity
import com.attentionmanager.domain.repository.DigestRepository
import kotlinx.coroutines.flow.Flow

class DigestRepositoryImpl(
    private val dao: DigestDao
) : DigestRepository {
    override fun observeDigests(): Flow<List<DigestEntity>> = dao.observeDigests()

    override fun observeDigestCount(): Flow<Int> = dao.observeDigestCount()

    override suspend fun insert(digest: DigestEntity): Long = dao.insert(digest)
}
