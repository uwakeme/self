package com.self.profile.domain.model

import kotlinx.serialization.Serializable

/**
 * 提醒领域模型。
 */
@Serializable
data class Reminder(
    val id: String,
    val personId: String?,
    val type: ReminderType,
    val title: String,
    val date: Long,                          // epoch day
    val advanceDays: Int = 3,
    val reminderTime: String = "08:00",
    val repeat: RepeatRule = RepeatRule.YEARLY,
    val enabled: Boolean = true,
    val notes: String? = null,
    val triggeredAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val isLunar: Boolean = false,
    val deletedAt: Long? = null
)

@Serializable
enum class ReminderType(val label: String) {
    BIRTHDAY("生日"),
    ANNIVERSARY("纪念日"),
    CUSTOM("自定义");

    companion object {
        fun fromLabel(label: String): ReminderType =
            values().firstOrNull { it.label == label } ?: CUSTOM
    }
}

@Serializable
enum class RepeatRule(val label: String) {
    NONE("不重复"),
    YEARLY("每年"),
    MONTHLY("每月"),
    WEEKLY("每周");

    companion object {
        fun fromLabel(label: String): RepeatRule =
            values().firstOrNull { it.label == label } ?: YEARLY
    }
}
