package com.codebyramiz.breathefocus.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.codebyramiz.breathefocus.core.data.ActiveSessionStore
import com.codebyramiz.breathefocus.core.data.DataStoreSettingsRepository
import com.codebyramiz.breathefocus.core.data.RoomSessionRepository
import com.codebyramiz.breathefocus.core.data.SessionOutbox
import com.codebyramiz.breathefocus.core.data.SessionRepository
import com.codebyramiz.breathefocus.core.data.SettingsRepository
import com.codebyramiz.breathefocus.core.data.db.BreatheDatabase
import com.codebyramiz.breathefocus.core.data.db.SessionDao
import com.codebyramiz.breathefocus.core.data.sync.DataLayerReader
import com.codebyramiz.breathefocus.core.data.sync.DataLayerSync
import com.codebyramiz.breathefocus.core.data.sync.WearableDataLayerReader
import com.codebyramiz.breathefocus.core.data.sync.WearableDataLayerSync
import com.codebyramiz.breathefocus.core.data.time.BroadcastTimeChanges
import com.codebyramiz.breathefocus.core.data.time.TimeChanges
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds abstract fun sessionRepository(impl: RoomSessionRepository): SessionRepository

    @Binds abstract fun settingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds abstract fun dataLayerSync(impl: WearableDataLayerSync): DataLayerSync

    @Binds abstract fun dataLayerReader(impl: WearableDataLayerReader): DataLayerReader

    @Binds abstract fun timeChanges(impl: BroadcastTimeChanges): TimeChanges

    @Binds abstract fun activeSessionStore(impl: RoomSessionRepository): ActiveSessionStore

    @Binds abstract fun sessionOutbox(impl: RoomSessionRepository): SessionOutbox

    companion object {
        @Provides
        @Singleton
        fun database(@ApplicationContext context: Context): BreatheDatabase =
            Room.databaseBuilder(context, BreatheDatabase::class.java, BreatheDatabase.NAME)
                .addMigrations(BreatheDatabase.MIGRATION_1_2, BreatheDatabase.MIGRATION_2_3)
                .build()

        @Provides fun sessionDao(db: BreatheDatabase): SessionDao = db.sessionDao()

        @Provides
        @Singleton
        fun preferences(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }

        @Provides fun dataClient(@ApplicationContext context: Context): DataClient = Wearable.getDataClient(context)

        @Provides fun nodeClient(@ApplicationContext context: Context): NodeClient = Wearable.getNodeClient(context)

        /** Follows the system time zone as it changes, unlike [Clock.systemDefaultZone], which fixes it at creation. */
        @Provides
        fun clock(): Clock = object : Clock() {
            override fun getZone(): ZoneId = ZoneId.systemDefault()

            override fun withZone(zone: ZoneId): Clock = system(zone)

            override fun instant(): Instant = Instant.now()
        }
    }
}
