package com.self.profile.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.self.profile.data.repository.PersonRepository
import com.self.profile.data.repository.RecycleBinJanitor
import com.self.profile.data.repository.ReminderRepository
import com.self.profile.domain.model.Person
import com.self.profile.domain.model.Reminder
import com.self.profile.util.PhotoStorage
import com.self.profile.util.ReminderCalc
import com.self.profile.work.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 回收站 ViewModel：查看软删的人物 / 提醒，支持恢复与彻底删除。
 */
@HiltViewModel
class RecycleBinViewModel @Inject constructor(
    private val personRepo: PersonRepository,
    private val reminderRepo: ReminderRepository,
    private val photoStorage: PhotoStorage,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val state: StateFlow<RecycleBinUiState> = combine(
        personRepo.observeDeleted(),
        reminderRepo.observeDeleted()
    ) { persons, reminders ->
        RecycleBinUiState(
            people = persons,
            reminders = reminders,
            isEmpty = persons.isEmpty() && reminders.isEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecycleBinUiState.empty()
    )

    /**
     * 恢复人物：人物 + 「随人物一起被删」的提醒回到在册，已开启的提醒重新挂调度。
     *
     * 只恢复 deletedAt 与人物一致的提醒（人物软删时同一时间戳连带软删）；
     * 用户更早单独删进回收站的提醒保持软删，不被连带复活。
     */
    fun restorePerson(person: Person) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            personRepo.restore(person.id, now)
            val personDeletedAt = person.deletedAt
            reminderRepo.getByPersonAnyState(person.id)
                .filter { r -> personDeletedAt != null && r.deletedAt == personDeletedAt }
                .forEach { r ->
                    reminderRepo.restore(r.id, now)
                    if (r.enabled) {
                        val next = ReminderCalc.nextTriggerMillis(r)
                        if (next != null) {
                            ReminderScheduler.scheduleReminder(appContext, r.id, next)
                        } else {
                            // 一次性且已过期：恢复为在册但关闭，不留永不触发的僵尸
                            reminderRepo.setEnabled(r.id, false, now)
                        }
                    }
                }
        }
    }

    /** 彻底删除人物：关联提醒硬删 + 取消调度 + 照片文件删除，不可恢复 */
    fun purgePerson(person: Person) {
        viewModelScope.launch {
            purgePersonInternal(person)
        }
    }

    /** 恢复提醒并重新调度；一次性且已过期的恢复为在册但关闭 */
    fun restoreReminder(reminder: Reminder) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            reminderRepo.restore(reminder.id, now)
            if (reminder.enabled) {
                val next = ReminderCalc.nextTriggerMillis(reminder)
                if (next != null) {
                    ReminderScheduler.scheduleReminder(appContext, reminder.id, next)
                } else {
                    reminderRepo.setEnabled(reminder.id, false, now)
                }
            }
        }
    }

    fun purgeReminder(reminder: Reminder) {
        viewModelScope.launch {
            ReminderScheduler.cancel(appContext, reminder.id)
            reminderRepo.delete(reminder.id)
        }
    }

    /** 清空回收站 */
    fun emptyBin() {
        viewModelScope.launch {
            personRepo.observeDeleted().first().forEach { purgePersonInternal(it) }
            reminderRepo.observeDeleted().first().forEach { r ->
                ReminderScheduler.cancel(appContext, r.id)
                reminderRepo.delete(r.id)
            }
        }
    }

    private suspend fun purgePersonInternal(person: Person) {
        reminderRepo.getByPersonAnyState(person.id).forEach { r ->
            ReminderScheduler.cancel(appContext, r.id)
            reminderRepo.delete(r.id)
        }
        personRepo.delete(person.id)
        person.photos.forEach { photoStorage.delete(it.url) }
    }
}

data class RecycleBinUiState(
    val people: List<Person>,
    val reminders: List<Reminder>,
    val isEmpty: Boolean
) {
    companion object {
        fun empty() = RecycleBinUiState(emptyList(), emptyList(), true)
    }
}
