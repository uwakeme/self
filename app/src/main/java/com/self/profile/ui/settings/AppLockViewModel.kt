package com.self.profile.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.self.profile.data.datastore.SettingsDataStore
import com.self.profile.ui.theme.DarkModeOption
import com.self.profile.ui.theme.FontScale
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 应用级偏好（深色模式 / 字体大小 / 应用锁）的全局 ViewModel。
 *
 * 跨页面共享。从 SettingsDataStore 读取 darkMode / fontScale，
 * 改变后 UI 立即刷新。
 */
@HiltViewModel
class AppLockViewModel @Inject constructor(
    settingsStore: SettingsDataStore
) : ViewModel() {

    val darkMode: StateFlow<DarkModeOption> = settingsStore.settings
        .map { s ->
            when (s.darkMode) {
                com.self.profile.domain.model.DarkModePref.OFF -> DarkModeOption.Off
                com.self.profile.domain.model.DarkModePref.ON -> DarkModeOption.On
                com.self.profile.domain.model.DarkModePref.SYSTEM -> DarkModeOption.System
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DarkModeOption.System
        )

    val fontScale: StateFlow<FontScale> = settingsStore.settings
        .map { s ->
            when (s.fontScale) {
                com.self.profile.domain.model.FontSizePref.SMALL -> FontScale.SMALL
                com.self.profile.domain.model.FontSizePref.STANDARD -> FontScale.STANDARD
                com.self.profile.domain.model.FontSizePref.LARGE -> FontScale.LARGE
                com.self.profile.domain.model.FontSizePref.XLARGE -> FontScale.XLARGE
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FontScale.STANDARD
        )

    val appLockEnabled: StateFlow<Boolean> = settingsStore.settings
        .map { it.appLockEnabled }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )
}
