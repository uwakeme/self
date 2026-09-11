package com.self.profile.di

import android.content.Context
import androidx.room.Room
import com.self.profile.data.local.AppDatabase
import com.self.profile.data.local.dao.PersonDao
import com.self.profile.data.local.dao.ReminderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库 Hilt 模块。提供 Room AppDatabase 与 DAO。
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DB_NAME
        )
            // v2 起正式迁移：不再破坏性清库，升版本必须配套新增 Migration
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun providePersonDao(db: AppDatabase): PersonDao = db.personDao()

    @Provides
    fun provideReminderDao(db: AppDatabase): ReminderDao = db.reminderDao()
}
