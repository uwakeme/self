package com.self.profile.ui.settings

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.self.profile.ui.theme.WarmJournalColors
import kotlinx.coroutines.delay

/**
 * 锁屏覆盖层（US-018 / PRD AC-018-5 / AC-018-6）。
 *
 * 出现条件：AppLockManager.isLocked = true
 *
 * 行为：
 * - 自动调起 BiometricPrompt
 * - API 30+：默认 [BIOMETRIC_STRONG | DEVICE_CREDENTIAL]（系统 PIN 作为生物识别失败后的兜底）
 * - API 26-29：[BIOMETRIC_WEAK]（老版本不支持 device credential 组合）
 * - 失败重试降级（由 [state] 和 [callbacks] 驱动）：
 *   - 生物识别连续失败 3 次 → 切到「只允许设备密码」模式
 *   - 设备密码连续失败 5 次 → 锁定 30 秒，倒计时归零后自动重置 + 重弹 prompt
 */
@Composable
fun LockScreen(
    state: LockScreenState,
    callbacks: LockScreenCallbacks
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    val inLockout = state.lockoutRemainingMs != null && state.lockoutRemainingMs > 0L

    fun showPrompt() {
        if (activity == null) return
        if (inLockout) return  // 锁定期内不弹

        val allowedAuthenticators = computeAllowedAuthenticators(
            requireDeviceCredentialOnly = state.requireDeviceCredentialOnly
        )

        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    callbacks.onUnlocked()
                }

                override fun onAuthenticationFailed() {
                    // 单次失败（指纹不对、密码错位等）
                    if (state.requireDeviceCredentialOnly) {
                        callbacks.onDeviceCredentialFailed()
                    } else {
                        callbacks.onBiometricFailed()
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_CANCELED -> {
                            // 用户主动取消
                            callbacks.onUserCancelled()
                        }
                        BiometricPrompt.ERROR_LOCKOUT,
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                            // 系统级锁定（通常 30s），同步到我们这里
                            callbacks.onSystemLockout()
                        }
                        else -> {
                            // 其他错误（如 NO_BIOMETRICS / NO_DEVICE_CREDENTIAL）算失败
                            if (state.requireDeviceCredentialOnly) {
                                callbacks.onDeviceCredentialFailed()
                            } else {
                                callbacks.onBiometricFailed()
                            }
                        }
                    }
                }
            }
        )

        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("解锁自我档案")
            .setSubtitle(
                if (state.requireDeviceCredentialOnly) "请使用设备 PIN / 密码" else "验证以继续"
            )
            .setAllowedAuthenticators(allowedAuthenticators)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
            allowedAuthenticators == BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) {
            // 单独 BIOMETRIC_WEAK 或老版本必须设 negative button
            builder.setNegativeButtonText("取消")
        }
        biometricPrompt.authenticate(builder.build())
    }

    // 首次进入自动唤起
    LaunchedEffect(Unit) { showPrompt() }

    // 当 requireDeviceCredentialOnly 变化时重弹（让 UI 切到正确的 prompt 模式）
    LaunchedEffect(state.requireDeviceCredentialOnly) {
        if (state.requireDeviceCredentialOnly && !inLockout) {
            // 短暂延迟让上一个 prompt 完全 dismiss
            delay(300)
            showPrompt()
        }
    }

    // 锁定期归零时自动重置 + 重弹
    LaunchedEffect(state.lockoutRemainingMs) {
        if (state.lockoutRemainingMs == 0L) {
            callbacks.onLockoutExpired()
            delay(300)
            showPrompt()
        }
    }

    // ====== UI ======

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(WarmJournalColors.PrimaryLightest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.requireDeviceCredentialOnly) Icons.Filled.Pin else Icons.Filled.Lock,
                    contentDescription = null,
                    tint = WarmJournalColors.Primary,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "自我档案与人物图鉴",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (inLockout) {
                    "请稍后再试"
                } else if (state.requireDeviceCredentialOnly) {
                    "请使用设备 PIN / 密码解锁"
                } else {
                    "应用锁已启用"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // 倒计时
            if (inLockout) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = formatRemaining(state.lockoutRemainingMs ?: 0L),
                    style = MaterialTheme.typography.displaySmall,
                    color = WarmJournalColors.Primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "由于多次验证失败，应用暂时锁定",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // 失败提示（非锁定期）
            if (!inLockout) {
                val fails = if (state.requireDeviceCredentialOnly) {
                    state.deviceCredentialFailCount
                } else {
                    state.biometricFailCount
                }
                if (fails > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (state.requireDeviceCredentialOnly) {
                            "已失败 $fails / ${AppLockManager.DEVICE_CREDENTIAL_FAIL_THRESHOLD} 次"
                        } else {
                            "已失败 $fails / ${AppLockManager.BIOMETRIC_FAIL_THRESHOLD} 次"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // 锁定期内不显示解锁按钮
            if (!inLockout) {
                Button(
                    onClick = { showPrompt() },
                    enabled = activity != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WarmJournalColors.Primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (state.requireDeviceCredentialOnly) Icons.Filled.Pin else Icons.Filled.Fingerprint,
                        contentDescription = null
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(if (state.requireDeviceCredentialOnly) "输入 PIN" else "点击解锁")
                }
            }
        }
    }
}

/** 锁屏 UI 状态（由 AppLockManager 提供）。 */
data class LockScreenState(
    val requireDeviceCredentialOnly: Boolean,
    val lockoutRemainingMs: Long?,
    val biometricFailCount: Int,
    val deviceCredentialFailCount: Int
) {
    companion object {
        val Initial = LockScreenState(
            requireDeviceCredentialOnly = false,
            lockoutRemainingMs = null,
            biometricFailCount = 0,
            deviceCredentialFailCount = 0
        )
    }
}

/** 锁屏回调（由 AppLockManager 提供）。 */
data class LockScreenCallbacks(
    val onUnlocked: () -> Unit,
    val onBiometricFailed: () -> Unit,
    val onDeviceCredentialFailed: () -> Unit,
    val onSystemLockout: () -> Unit,
    val onUserCancelled: () -> Unit,
    val onLockoutExpired: () -> Unit
)

private fun computeAllowedAuthenticators(requireDeviceCredentialOnly: Boolean): Int {
    val isApi30Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    return when {
        // API 30+ 且强制只用设备密码
        isApi30Plus && requireDeviceCredentialOnly ->
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        // API 30+ 默认：生物 + 设备密码（用户在 prompt 内可选）
        isApi30Plus ->
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        // API 26-29 只能用 BIOMETRIC_WEAK
        else -> BiometricManager.Authenticators.BIOMETRIC_WEAK
    }
}

private fun formatRemaining(ms: Long): String {
    val seconds = ((ms + 999L) / 1000L).coerceAtLeast(0L)
    return "${seconds} 秒后重试"
}
