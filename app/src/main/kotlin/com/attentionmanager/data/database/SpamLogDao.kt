package com.attentionmanager.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpamLogDao {
    @Query("SELECT * FROM spam_log ORDER BY timestamp DESC")
    fun observeSpamLog(): Flow<List<SpamLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SpamLogEntity)

    @Query("DELETE FROM spam_log")
    suspend fun clearAll()
}
