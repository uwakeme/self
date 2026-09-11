package com.self.profile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.self.profile.data.local.dao.PersonDao
import com.self.profile.data.local.dao.ReminderDao
import com.self.profile.data.local.entity.PersonEntity
import com.self.profile.data.local.entity.ReminderEntity

/**
 * 本地数据库。
 *
 * MVP 阶段曾用 `fallbackToDestructiveMigration`（升级清库）；v2 起用户数据是真实资产，
 * 改为正式 [Migration]：缺迁移宁可崩溃也不静默清库。每次升 version 必须在
 * [DatabaseModule] 注册对应 Migration。
 */
@Database(
    entities = [
        PersonEntity::class,
        ReminderEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        const val DB_NAME = "self_profile.db"

        /** v1 → v2：新增软删除（回收站）、农历提醒标记、人物自定义标签 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE persons ADD COLUMN deleted_at INTEGER")
                db.execSQL("ALTER TABLE persons ADD COLUMN tags_json TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE reminders ADD COLUMN is_lunar INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE reminders ADD COLUMN deleted_at INTEGER")
            }
        }
    }
}
