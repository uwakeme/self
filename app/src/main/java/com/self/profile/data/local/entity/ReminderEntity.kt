package com.self.profile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 提醒实体。
 *
 * 关联 [PersonEntity.id]（可空，null 表示自定义提醒）。
 * 提醒到点由 ReminderWorker 触发；状态由 [enabled] 控制。
 */
@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["person_id"]),
        Index(value = ["date"]),
        Index(value = ["enabled"])
    ]
)
data class ReminderEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "person_id")
    val personId: String? = null,                         // null = 自定义提醒

    @ColumnInfo(name = "type")
    val type: String,                                     // 生日 / 纪念日 / 自定义

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "date")
    val date: Long,                                       // epoch day

    @ColumnInfo(name = "advance_days")
    val advanceDays: Int = 3,                             // 0-30

    @ColumnInfo(name = "reminder_time")
    val reminderTime: String = "08:00",                   // HH:mm

    @ColumnInfo(name = "repeat")
    val repeat: String = "每年",                          // 不重复/每年/每月/每周

    @ColumnInfo(name = "enabled")
    val enabled: Boolean = true,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "triggered_at")
    val triggeredAt: Long? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
