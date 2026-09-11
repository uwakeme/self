package com.self.profile.domain.model

import kotlinx.serialization.Serializable

/**
 * 应用设置领域模型（对应 PRD §3.2.3）。
 */
@Serializable
data class AppSettings(
    val userNickname: String = "用户",
    val userAvatar: String? = null,
    val defaultAdvanceDays: Int = 3,
    val defaultReminderTime: String = "08:00",
    val showLunarBirthday: Boolean = false,
    val darkMode: DarkModePref = DarkModePref.OFF,
    val fontScale: FontSizePref = FontSizePref.STANDARD,
    val lastBackupTime: Long? = null,
    val selfProfileModuleOrder: List<Int> = defaultSelfModuleOrder,
    val personDetailModuleOrder: List<Int> = defaultPersonModuleOrder,
    val gallerySortRule: GallerySortRule = GallerySortRule.CREATED_AT,
    val galleryCustomOrder: List<String>? = null,
    val appLockEnabled: Boolean = false,
    val appLockTimeout: Int = 5
) {
    companion object {
        val defaultSelfModuleOrder: List<Int> = (1..15).toList()
        val defaultPersonModuleOrder: List<Int> = (1..17).toList()
    }
}

@Serializable
enum class DarkModePref(val storage: String) {
    OFF("off"),
    ON("on"),
    SYSTEM("system");

    companion object {
        fun fromStorage(s: String): DarkModePref =
            values().firstOrNull { it.storage == s } ?: OFF
    }
}

@Serializable
enum class FontSizePref(val storage: String) {
    SMALL("small"),
    STANDARD("standard"),
    LARGE("large"),
    XLARGE("xlarge");

    companion object {
        fun fromStorage(s: String): FontSizePref =
            values().firstOrNull { it.storage == s } ?: STANDARD
    }
}

@Serializable
enum class GallerySortRule(val storage: String, val label: String) {
    RECENTLY_VIEWED("recently_viewed", "最近查看"),
    RECENTLY_MODIFIED("recently_modified", "最近修改"),
    CREATED_AT("created_at", "创建时间"),
    CUSTOM("custom", "自定义");

    companion object {
        fun fromStorage(s: String): GallerySortRule =
            values().firstOrNull { it.storage == s } ?: CREATED_AT
    }
}
