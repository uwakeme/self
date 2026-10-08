package com.self.profile.ui.settings

import android.os.SystemClock
import com.self.profile.data.datastore.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用锁管理器（US-018）。
 *
 * 职责：
 * - 跟踪 App 是否被锁定
 * - 跟踪 App 进入后台的时间戳，回到前台时根据 [appLockTimeout] 决定是否锁定（默认 5s）
 * - 失败重试 / 降级状态机（PRD AC-018-5）：
 *   - 生物识别连续失败 3 次 → 切到「只允许设备密码」模式（API 30+）
 *   - 设备密码连续失败 5 次 → 锁定 30 秒，倒计时归零后重置
 *   - 系统级 lockout（BiometricPrompt.ERROR_LOCKOUT）→ 同步进入 30s 锁定
 *
 * UI 通过 [requireDeviceCredentialOnly] 和 [lockoutRemainingMs] 决定显示哪种 prompt 和倒计时。
 */
@Singleton
class AppLockManager @Inject constructor(
    private val settingsStore: SettingsDataStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ====== 锁定状态 ======

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var lastBackgroundedAt: Long? = null

    // ====== 失败计数 / 降级状态 ======

    private val _biometricFailCount = MutableStateFlow(0)
    val biometricFailCount: StateFlow<Int> = _biometricFailCount.asStateFlow()

    private val _deviceCredentialFailCount = MutableStateFlow(0)
    val deviceCredentialFailCount: StateFlow<Int> = _deviceCredentialFailCount.asStateFlow()

    private val _lockoutRemainingMs = MutableStateFlow<Long?>(null)
    /**
     * 倒计时剩余毫秒。`null` 表示未锁定；`> 0` 表示在锁定期内；`== 0L` 表示刚刚归零（UI 用来触发重试）。
     */
    val lockoutRemainingMs: StateFlow<Long?> = _lockoutRemainingMs.asStateFlow()

    val requireDeviceCredentialOnly: StateFlow<Boolean> = _biometricFailCount
        .map { it >= BIOMETRIC_FAIL_THRESHOLD }
        .stateIn(scope, SharingStarted.Eagerly, false)

    private var lockoutUntil: Long? = null
    private var lockoutJob: Job? = null

    /**
     * App 进入后台。记时间戳供 [onAppForegrounded] 比对。
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
     * 解锁成功。重置所有失败计数和锁定期。
     */
    fun unlock() {
        lastBackgroundedAt = null
        _isLocked.value = false
        resetFailCounters()
    }

    /**
     * 设置变更时（开关被打开），立即锁定以让用户必须先解锁一次。
     */
    fun onLockEnabledChanged(enabled: Boolean) {
        if (enabled && !_isLocked.value) {
            _isLocked.value = true
        }
    }

    // ====== 失败回调（由 LockScreen 在 BiometricPrompt 回调里调） ======

    /**
     * 生物识别失败一次。失败达 [BIOMETRIC_FAIL_THRESHOLD] 次后，
     * [requireDeviceCredentialOnly] 翻成 true，UI 应切到只允许设备密码。
     */
    fun onBiometricFailed() {
        if (isInLockout()) return
        _biometricFailCount.value = _biometricFailCount.value + 1
    }

    /**
     * 设备密码失败一次。失败达 [DEVICE_CREDENTIAL_FAIL_THRESHOLD] 次后进入 [LOCKOUT_DURATION_MS] 锁定。
     */
    fun onDeviceCredentialFailed() {
        if (isInLockout()) return
        val newCount = _deviceCredentialFailCount.value + 1
        _deviceCredentialFailCount.value = newCount
        if (newCount >= DEVICE_CREDENTIAL_FAIL_THRESHOLD) {
            startLockout()
        }
    }

    /**
     * 系统级 lockout（BiometricPrompt.ERROR_LOCKOUT / ERROR_LOCKOUT_PERMANENT）。
     * 也进入我们的 30s 锁定。
     */
    fun onSystemLockout() {
        startLockout()
    }

    /**
     * 用户主动取消（点击取消/点空白处），不计失败次数。
     */
    fun onUserCancelled() {
        // 不重置计数 —— 下次再试算同一次会话
    }

    /**
     * 倒计时归零时被 UI 调用（也作为内部 state 变化的通知），
     * 重置失败计数让用户可以重试。
     */
    fun onLockoutExpired() {
        lockoutUntil = null
        _lockoutRemainingMs.value = null
        _biometricFailCount.value = 0
        _deviceCredentialFailCount.value = 0
    }

    // ====== 内部 ======

    private fun isInLockout(): Boolean {
        val until = lockoutUntil ?: return false
        return SystemClock.elapsedRealtime() < until
    }

    private fun startLockout() {
        cancelLockoutJob()
        val now = SystemClock.elapsedRealtime()
        val until = now + LOCKOUT_DURATION_MS
        lockoutUntil = until
        _lockoutRemainingMs.value = LOCKOUT_DURATION_MS
        lockoutJob = scope.launch {
            while (true) {
                val remaining = until - SystemClock.elapsedRealtime()
                if (remaining <= 0) {
                    // 先发 0L 再发 null：StateFlow 会合并同步连续赋值，
                    // 中间停一拍保证 UI 能观察到 0L（LockScreen 靠它触发自动重弹）
                    _lockoutRemainingMs.value = 0L
                    delay(200)
                    onLockoutExpired()
                    return@launch
                }
                _lockoutRemainingMs.value = remaining
                delay(1_000L)
            }
        }
    }

    private fun cancelLockoutJob() {
        lockoutJob?.cancel()
        lockoutJob = null
    }

    private fun resetFailCounters() {
        _biometricFailCount.value = 0
        _deviceCredentialFailCount.value = 0
        lockoutUntil = null
        _lockoutRemainingMs.value = null
        cancelLockoutJob()
    }

    companion object {
        /** 连续失败 3 次生物识别后强制只允许设备密码（API 30+） */
        const val BIOMETRIC_FAIL_THRESHOLD = 3

        /** 连续失败 5 次设备密码后锁定 30 秒 */
        const val DEVICE_CREDENTIAL_FAIL_THRESHOLD = 5

        /** 锁定时长：30 秒 */
        const val LOCKOUT_DURATION_MS = 30_000L
    }
}
