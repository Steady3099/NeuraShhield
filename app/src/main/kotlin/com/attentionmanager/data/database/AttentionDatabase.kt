package com.attentionmanager.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        NotificationEntity::class,
        ContactPriorityEntity::class,
        DigestEntity::class,
        SpamLogEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(
    PriorityTierConverter::class,
    LongListConverter::class,
    StringListConverter::class
)
abstract class AttentionDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun contactPriorityDao(): ContactPriorityDao
    abstract fun digestDao(): DigestDao
    abstract fun spamLogDao(): SpamLogDao
}
