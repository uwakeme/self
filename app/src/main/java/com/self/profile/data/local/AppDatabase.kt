package com.self.profile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.self.profile.data.local.dao.PersonDao
import com.self.profile.data.local.dao.ReminderDao
import com.self.profile.data.local.entity.PersonEntity
import com.self.profile.data.local.entity.ReminderEntity

/**
 * 本地数据库。schema version = 1。
 *
 * MVP 阶段用 [androidx.room.Room.databaseBuilder] + `fallbackToDestructiveMigration`：
 * - 升级时直接清空重建（用户数据会丢，PRD §4.2 风险已记录）
 * - 后续上 [androidx.room.migration.Migration] 做平滑迁移
 */
@Database(
    entities = [
        PersonEntity::class,
        ReminderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        const val DB_NAME = "self_profile.db"
    }
}
