package com.attentionmanager.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactPriorityDao {
    @Query("SELECT * FROM contact_priorities ORDER BY lastUpdated DESC")
    fun observeContactPriorities(): Flow<List<ContactPriorityEntity>>

    @Query(
        """
        SELECT * FROM contact_priorities
        WHERE lower(displayName) = lower(:sender) AND packageName = :packageName
        LIMIT 1
        """
    )
    suspend fun findBySender(sender: String, packageName: String): ContactPriorityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ContactPriorityEntity)

    @Query("DELETE FROM contact_priorities")
    suspend fun clearAll()
}
