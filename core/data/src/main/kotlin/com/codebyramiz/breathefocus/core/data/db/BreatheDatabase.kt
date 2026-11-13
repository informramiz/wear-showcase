package com.codebyramiz.breathefocus.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SessionEntity::class], version = 1, exportSchema = true)
abstract class BreatheDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    companion object {
        const val NAME = "breathe.db"
    }
}
