package com.self.profile.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.self.profile.domain.model.AppSettings
import com.self.profile.domain.model.DarkModePref
import com.self.profile.domain.model.FontSizePref
import com.self.profile.domain.model.GallerySortRule
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings"
)

/**
 * 应用设置 DataStore。
 *
 * 阶段 1：包装 Preferences DataStore，暴露 Flow<AppSettings> 和更新方法。
 * 阶段 6+：添加应用锁密码 hash、备份时间戳等。
 */
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val store get() = context.settingsDataStore

    val settings: Flow<AppSettings> = store.data.map { p ->
        AppSettings(
            userNickname = p[Keys.USER_NICKNAME] ?: "用户",
            userAvatar = p[Keys.USER_AVATAR],
            defaultAdvanceDays = p[Keys.DEFAULT_ADVANCE_DAYS] ?: 3,
            defaultReminderTime = p[Keys.DEFAULT_REMINDER_TIME] ?: "08:00",
            showLunarBirthday = p[Keys.SHOW_LUNAR] ?: false,
            darkMode = DarkModePref.fromStorage(p[Keys.DARK_MODE] ?: "off"),
            fontScale = FontSizePref.fromStorage(p[Keys.FONT_SCALE] ?: "standard"),
            lastBackupTime = p[Keys.LAST_BACKUP],
            selfProfileModuleOrder = p[Keys.SELF_MODULE_ORDER]
                ?.split(",")?.mapNotNull { it.toIntOrNull() }
                ?.takeIf { it.isNotEmpty() } ?: AppSettings.defaultSelfModuleOrder,
            personDetailModuleOrder = p[Keys.PERSON_MODULE_ORDER]
                ?.split(",")?.mapNotNull { it.toIntOrNull() }
                ?.takeIf { it.isNotEmpty() } ?: AppSettings.defaultPersonModuleOrder,
            gallerySortRule = GallerySortRule.fromStorage(p[Keys.GALLERY_SORT] ?: "created_at"),
            galleryCustomOrder = p[Keys.GALLERY_CUSTOM_ORDER]
                ?.split(",")?.filter(String::isNotBlank),
            appLockEnabled = p[Keys.APP_LOCK_ENABLED] ?: false,
            appLockTimeout = p[Keys.APP_LOCK_TIMEOUT] ?: 5
        )
    }

    suspend fun setDarkMode(value: DarkModePref) = store.edit { it[Keys.DARK_MODE] = value.storage }
    suspend fun setFontScale(value: FontSizePref) = store.edit { it[Keys.FONT_SCALE] = value.storage }
    suspend fun setShowLunarBirthday(value: Boolean) = store.edit { it[Keys.SHOW_LUNAR] = value }
    suspend fun setUserNickname(value: String) = store.edit { it[Keys.USER_NICKNAME] = value }
    suspend fun setLastBackupTime(value: Long) = store.edit { it[Keys.LAST_BACKUP] = value }
    suspend fun setDefaultAdvanceDays(value: Int) = store.edit { it[Keys.DEFAULT_ADVANCE_DAYS] = value }
    suspend fun setDefaultReminderTime(value: String) = store.edit { it[Keys.DEFAULT_REMINDER_TIME] = value }
    suspend fun setAppLockEnabled(value: Boolean) = store.edit { it[Keys.APP_LOCK_ENABLED] = value }
    suspend fun setAppLockTimeout(value: Int) = store.edit { it[Keys.APP_LOCK_TIMEOUT] = value }
    suspend fun setGallerySortRule(value: GallerySortRule) = store.edit { it[Keys.GALLERY_SORT] = value.storage }
    suspend fun setGalleryCustomOrder(ids: List<String>) = store.edit {
        it[Keys.GALLERY_CUSTOM_ORDER] = ids.joinToString(",")
    }
    suspend fun setSelfProfileModuleOrder(order: List<Int>) = store.edit {
        it[Keys.SELF_MODULE_ORDER] = order.joinToString(",")
    }
    suspend fun setPersonDetailModuleOrder(order: List<Int>) = store.edit {
        it[Keys.PERSON_MODULE_ORDER] = order.joinToString(",")
    }

    /**
     * 从备份恢复设置。应用锁开关/超时是设备侧安全状态、lastBackupTime 是导出动作的
     * 时间戳，均不随备份恢复，保持当前设备值。
     */
    suspend fun restoreFrom(other: AppSettings) {
        store.edit {
            it[Keys.USER_NICKNAME] = other.userNickname
            if (other.userAvatar != null) it[Keys.USER_AVATAR] = other.userAvatar else it.remove(Keys.USER_AVATAR)
            it[Keys.DEFAULT_ADVANCE_DAYS] = other.defaultAdvanceDays
            it[Keys.DEFAULT_REMINDER_TIME] = other.defaultReminderTime
            it[Keys.SHOW_LUNAR] = other.showLunarBirthday
            it[Keys.DARK_MODE] = other.darkMode.storage
            it[Keys.FONT_SCALE] = other.fontScale.storage
            it[Keys.SELF_MODULE_ORDER] = other.selfProfileModuleOrder.joinToString(",")
            it[Keys.PERSON_MODULE_ORDER] = other.personDetailModuleOrder.joinToString(",")
            it[Keys.GALLERY_SORT] = other.gallerySortRule.storage
            if (other.galleryCustomOrder != null) {
                it[Keys.GALLERY_CUSTOM_ORDER] = other.galleryCustomOrder.joinToString(",")
            } else {
                it.remove(Keys.GALLERY_CUSTOM_ORDER)
            }
        }
    }

    private object Keys {
        val USER_NICKNAME = stringPreferencesKey("user_nickname")
        val USER_AVATAR = stringPreferencesKey("user_avatar")
        val DEFAULT_ADVANCE_DAYS = intPreferencesKey("default_advance_days")
        val DEFAULT_REMINDER_TIME = stringPreferencesKey("default_reminder_time")
        val SHOW_LUNAR = booleanPreferencesKey("show_lunar_birthday")
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val FONT_SCALE = stringPreferencesKey("font_scale")
        val LAST_BACKUP = longPreferencesKey("last_backup_time")
        val SELF_MODULE_ORDER = stringPreferencesKey("self_module_order")
        val PERSON_MODULE_ORDER = stringPreferencesKey("person_module_order")
        val GALLERY_SORT = stringPreferencesKey("gallery_sort_rule")
        val GALLERY_CUSTOM_ORDER = stringPreferencesKey("gallery_custom_order")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val APP_LOCK_TIMEOUT = intPreferencesKey("app_lock_timeout")
    }
}
