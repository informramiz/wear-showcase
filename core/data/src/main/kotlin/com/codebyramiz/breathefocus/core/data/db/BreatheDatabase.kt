package com.codebyramiz.breathefocus.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SessionEntity::class, ActiveSessionEntity::class, OutboxEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class BreatheDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    companion object {
        const val NAME = "breathe.db"

        /** Records the boot a session started in, so recovery can tell a reboot from a clock change. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `active_session` ADD COLUMN `bootCount` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Adds the active-session record and the publication outbox. Existing sessions are untouched. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `active_session` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, " +
                        "`pattern` TEXT, `startedAtElapsedRealtime` INTEGER NOT NULL, " +
                        "`startedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `session_outbox` (`sessionId` TEXT NOT NULL, PRIMARY KEY(`sessionId`))",
                )
            }
        }
    }
}
