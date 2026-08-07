package com.self.profile.ui.settings

import android.os.SystemClock
import com.self.profile.data.datastore.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用锁管理器（US-018）。
 *
 * 职责：
 * - 跟踪 App 是否被锁定
 * - 跟踪 App 进入后台的时间戳
 * - 回到前台时，根据 [timeoutSeconds] 决定是否锁定
 *
 * 锁定策略：
 * - 用户启用 appLockEnabled 后，首次启动就处于锁定状态（要解锁才能进）
 * - 后台超过 timeoutSeconds 秒后回前台 → 重新锁定
 * - 手动调用 [unlock] 解除锁定
 */
@Singleton
class AppLockManager @Inject constructor(
    private val settingsStore: SettingsDataStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var lastBackgroundedAt: Long? = null

    /**
     * App 进入后台。
     */
    fun onAppBackgrounded() {
        lastBackgroundedAt = SystemClock.elapsedRealtime()
    }

    /**
     * App 回到前台。如果超过锁定阈值（且 appLockEnabled），则进入锁定状态。
     */
    fun onAppForegrounded() {
        scope.launch {
            val settings = settingsStore.settings.first()
            if (!settings.appLockEnabled) return@launch
            val backgroundedAt = lastBackgroundedAt ?: run {
                // 首次启动：直接锁定
                _isLocked.value = true
                return@launch
            }
            val elapsed = SystemClock.elapsedRealtime() - backgroundedAt
            if (elapsed >= settings.appLockTimeout * 1000L) {
                _isLocked.value = true
            }
        }
    }

    /**
     * 立即锁定（手动触发，比如切换账户）。
     */
    fun lockNow() {
        _isLocked.value = true
    }

    /**
     * 解锁成功。
     */
    fun unlock() {
        lastBackgroundedAt = null
        _isLocked.value = false
    }

    /**
     * 设置变更时（开关被打开），立即锁定以让用户必须先解锁一次。
     */
    fun onLockEnabledChanged(enabled: Boolean) {
        if (enabled && !_isLocked.value) {
            _isLocked.value = true
        }
    }
}
