package com.self.profile

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.self.profile.ui.navigation.AppNav
import com.self.profile.ui.settings.AppLockManager
import com.self.profile.ui.settings.AppLockViewModel
import com.self.profile.ui.settings.LockScreen
import com.self.profile.ui.theme.SelfTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 唯一 Activity。所有页面走 Compose 导航。
 *
 * 启用 edge-to-edge 后让顶/底系统栏沉浸，背景延伸到状态栏之后。
 *
 * 继承 FragmentActivity 以支持 androidx.biometric.BiometricPrompt。
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var appLockManager: AppLockManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // 启动屏（必须在 super.onCreate 之前）
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsVm: AppLockViewModel = hiltViewModel()
            val darkMode by settingsVm.darkMode.collectAsState()
            val fontScale by settingsVm.fontScale.collectAsState()
            val isLocked by appLockManager.isLocked.collectAsState()

            SelfTheme(
                darkMode = darkMode,
                fontScale = fontScale
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNav()
                    }
                    if (isLocked) {
                        LockScreen(onUnlocked = { appLockManager.unlock() })
                    }
                }
            }
        }

        // 监听 App 前后台：进入后台记时间戳，回到前台时根据超时决定是否锁
        lifecycle.addObserver(androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> appLockManager.onAppBackgrounded()
                androidx.lifecycle.Lifecycle.Event.ON_START -> appLockManager.onAppForegrounded()
                else -> {}
            }
        })
    }
}
