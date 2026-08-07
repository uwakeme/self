package com.self.profile.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.self.profile.data.datastore.SettingsDataStore
import com.self.profile.data.repository.PersonRepository
import com.self.profile.data.repository.ReminderRepository
import com.self.profile.domain.model.Person
import com.self.profile.domain.model.Reminder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

/**
 * 首页 ViewModel。汇总问候语所需的数据：
 * - 自身（昵称、头像）
 * - 即将到来提醒（前 3 条）
 * - 关系概览（按类型计数 + 头像列表）
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val personRepo: PersonRepository,
    reminderRepo: ReminderRepository,
    settingsStore: SettingsDataStore
) : ViewModel() {

    private val today = LocalDate.now().toEpochDay()
    private val upcomingRange = today..(today + 30)  // 近期 = 未来 30 天

    val state: StateFlow<HomeUiState> = combine(
        personRepo.observeSelf(),
        personRepo.observeAll(),
        reminderRepo.observeUpcoming(today, today + 30),
        settingsStore.settings
    ) { self, allPeople, upcomingReminders, settings ->
        val others = allPeople.filter { !it.isSelf }
        HomeUiState(
            nickname = settings.userNickname.takeIf { it.isNotBlank() } ?: "用户",
            avatarUrl = settings.userAvatar,
            fallbackColor = self?.avatarColor ?: "#C4785B",
            fallbackInitial = self?.avatarInitial ?: "我",
            upcomingReminders = upcomingReminders.take(3),
            others = others,
            relationshipCounts = RelationshipCounts(
                family = others.count { it.relationshipType.label == "家人" },
                friend = others.count { it.relationshipType.label == "朋友" },
                colleague = others.count { it.relationshipType.label == "同事" },
                classmate = others.count { it.relationshipType.label == "同学" }
            ),
            showAllCount = others.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState.empty()
    )
}

data class HomeUiState(
    val nickname: String,
    val avatarUrl: String?,
    val fallbackColor: String,
    val fallbackInitial: String,
    val upcomingReminders: List<Reminder>,
    val others: List<Person>,
    val relationshipCounts: RelationshipCounts,
    val showAllCount: Int
) {
    companion object {
        fun empty() = HomeUiState(
            nickname = "用户",
            avatarUrl = null,
            fallbackColor = "#C4785B",
            fallbackInitial = "我",
            upcomingReminders = emptyList(),
            others = emptyList(),
            relationshipCounts = RelationshipCounts(0, 0, 0, 0),
            showAllCount = 0
        )
    }
}

data class RelationshipCounts(
    val family: Int,
    val friend: Int,
    val colleague: Int,
    val classmate: Int
) {
    val total: Int get() = family + friend + colleague + classmate
}
