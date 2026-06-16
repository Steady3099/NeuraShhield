package com.attentionmanager.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DigestDao {
    @Query("SELECT * FROM digests ORDER BY generatedAt DESC")
    fun observeDigests(): Flow<List<DigestEntity>>

    @Query("SELECT COUNT(*) FROM digests")
    fun observeDigestCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DigestEntity): Long
}
