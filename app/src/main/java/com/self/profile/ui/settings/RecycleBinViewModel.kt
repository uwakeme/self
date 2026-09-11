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

    /** 恢复人物：人物 + 其关联提醒一起回到在册，已开启的提醒重新挂调度 */
    fun restorePerson(person: Person) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            personRepo.restore(person.id, now)
            reminderRepo.getByPersonAnyState(person.id).forEach { r ->
                reminderRepo.restore(r.id, now)
                if (r.enabled) {
                    ReminderCalc.nextTriggerMillis(r)?.let {
                        ReminderScheduler.scheduleReminder(appContext, r.id, it)
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

    /** 恢复提醒并重新调度 */
    fun restoreReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderRepo.restore(reminder.id, System.currentTimeMillis())
            if (reminder.enabled) {
                ReminderCalc.nextTriggerMillis(reminder)?.let {
                    ReminderScheduler.scheduleReminder(appContext, reminder.id, it)
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
