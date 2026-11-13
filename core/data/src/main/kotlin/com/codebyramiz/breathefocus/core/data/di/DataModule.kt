package com.codebyramiz.breathefocus.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.codebyramiz.breathefocus.core.data.DataStoreSettingsRepository
import com.codebyramiz.breathefocus.core.data.RoomSessionRepository
import com.codebyramiz.breathefocus.core.data.SessionRepository
import com.codebyramiz.breathefocus.core.data.SettingsRepository
import com.codebyramiz.breathefocus.core.data.db.BreatheDatabase
import com.codebyramiz.breathefocus.core.data.db.SessionDao
import com.codebyramiz.breathefocus.core.data.sync.DataLayerSync
import com.codebyramiz.breathefocus.core.data.sync.DataStorePendingSessionSync
import com.codebyramiz.breathefocus.core.data.sync.PendingSessionSync
import com.codebyramiz.breathefocus.core.data.sync.WearableDataLayerSync
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.Wearable
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds abstract fun sessionRepository(impl: RoomSessionRepository): SessionRepository

    @Binds abstract fun settingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds abstract fun dataLayerSync(impl: WearableDataLayerSync): DataLayerSync

    @Binds abstract fun pendingSessionSync(impl: DataStorePendingSessionSync): PendingSessionSync

    companion object {
        @Provides
        @Singleton
        fun database(@ApplicationContext context: Context): BreatheDatabase =
            Room.databaseBuilder(context, BreatheDatabase::class.java, BreatheDatabase.NAME).build()

        @Provides fun sessionDao(db: BreatheDatabase): SessionDao = db.sessionDao()

        @Provides
        @Singleton
        fun preferences(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }

        @Provides fun dataClient(@ApplicationContext context: Context): DataClient = Wearable.getDataClient(context)

        @Provides fun clock(): Clock = Clock.systemDefaultZone()
    }
}
