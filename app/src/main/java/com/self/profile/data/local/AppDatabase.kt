package com.self.profile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.self.profile.data.local.dao.PersonDao
import com.self.profile.data.local.dao.ReminderDao
import com.self.profile.data.local.entity.PersonEntity
import com.self.profile.data.local.entity.ReminderEntity

/**
 * 本地数据库。
 *
 * 开发阶段（未上线）schema 允许随需求重置：v3 移除了 persons.closeness_level，
 * 走 fallbackToDestructiveMigration 直接重建（用户已确认开发期数据可随时清空）。
 * **上线前必须**：删掉 fallback、启用 exportSchema、为每个版本变更写正式 Migration。
 */
@Database(
    entities = [
        PersonEntity::class,
        ReminderEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        const val DB_NAME = "self_profile.db"
    }
}
