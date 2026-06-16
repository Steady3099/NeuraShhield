package com.attentionmanager.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

object DatabaseModule {
    private val migration1To2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE notifications ADD COLUMN notificationKey TEXT NOT NULL DEFAULT ''")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_notificationKey ON notifications(notificationKey)")
            deleteExactDuplicates(db)
        }
    }

    private val migration2To3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            deleteExactDuplicates(db)
            db.execSQL(
                """
                DELETE FROM notifications
                WHERE id IN (
                    SELECT summary.id
                    FROM notifications AS summary
                    WHERE EXISTS (
                        SELECT 1
                        FROM notifications AS item
                        WHERE item.id != summary.id
                            AND item.packageName = summary.packageName
                            AND ABS(item.timestamp - summary.timestamp) <= 15000
                    )
                    AND (
                        lower(summary.body) LIKE '%messages from%'
                        OR lower(summary.body) LIKE '%new messages%'
                        OR lower(summary.body) LIKE '%unread messages%'
                        OR lower(summary.body) LIKE '% chats%'
                        OR lower(summary.body) LIKE '% conversations%'
                        OR lower(summary.title) IN ('whatsapp', 'telegram', 'signal', 'messages', 'messenger')
                    )
                )
                """.trimIndent()
            )
        }
    }

    fun create(context: Context): AttentionDatabase {
        val appContext = context.applicationContext
        SQLiteDatabase.loadLibs(appContext)
        val passphrase = SqlCipherPassphraseProvider(appContext).getPassphrase()
        val factory = SupportFactory(passphrase)
        return Room.databaseBuilder(
            appContext,
            AttentionDatabase::class.java,
            "attention_manager.db"
        )
            .openHelperFactory(factory)
            .addMigrations(migration1To2, migration2To3)
            .fallbackToDestructiveMigration(false)
            .build()
    }

    private fun deleteExactDuplicates(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            DELETE FROM notifications
            WHERE id NOT IN (
                SELECT MAX(id)
                FROM notifications
                GROUP BY packageName, title, body, IFNULL(sender, ''), timestamp
            )
            """.trimIndent()
        )
    }
}
