package com.self.profile.data.repository

import com.self.profile.data.local.dao.ReminderDao
import com.self.profile.data.local.entity.ReminderEntity
import com.self.profile.domain.model.Reminder
import com.self.profile.domain.model.ReminderType
import com.self.profile.domain.model.RepeatRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 提醒仓库。
 */
@Singleton
class ReminderRepository @Inject constructor(
    private val dao: ReminderDao
) {

    fun observeAll(): Flow<List<Reminder>> = dao.observeAll().map { it.map(::toDomain) }

    fun observeByPerson(personId: String): Flow<List<Reminder>> =
        dao.observeByPerson(personId).map { it.map(::toDomain) }

    fun observeEnabled(): Flow<List<Reminder>> = dao.observeEnabled().map { it.map(::toDomain) }

    fun observeCount(): Flow<Int> = dao.observeCount()

    fun observeEnabledCount(): Flow<Int> = dao.observeEnabledCount()

    fun observeUpcoming(fromDate: Long, toDate: Long): Flow<List<Reminder>> =
        dao.observeUpcoming(fromDate, toDate).map { it.map(::toDomain) }

    suspend fun getById(id: String): Reminder? = dao.getById(id)?.let(::toDomain)

    suspend fun upsert(reminder: Reminder) = dao.insert(toEntity(reminder))

    suspend fun upsertAll(reminders: List<Reminder>) =
        dao.insertAll(reminders.map(::toEntity))

    suspend fun setEnabled(id: String, enabled: Boolean, now: Long) =
        dao.setEnabled(id, enabled, now)

    suspend fun delete(id: String) = dao.deleteById(id)

    private fun toDomain(e: ReminderEntity): Reminder = Reminder(
        id = e.id,
        personId = e.personId,
        type = ReminderType.fromLabel(e.type),
        title = e.title,
        date = e.date,
        advanceDays = e.advanceDays,
        reminderTime = e.reminderTime,
        repeat = RepeatRule.fromLabel(e.repeat),
        enabled = e.enabled,
        notes = e.notes,
        triggeredAt = e.triggeredAt,
        createdAt = e.createdAt,
        updatedAt = e.updatedAt
    )

    private fun toEntity(r: Reminder): ReminderEntity = ReminderEntity(
        id = r.id,
        personId = r.personId,
        type = r.type.label,
        title = r.title,
        date = r.date,
        advanceDays = r.advanceDays,
        reminderTime = r.reminderTime,
        repeat = r.repeat.label,
        enabled = r.enabled,
        notes = r.notes,
        triggeredAt = r.triggeredAt,
        createdAt = r.createdAt,
        updatedAt = r.updatedAt
    )
}
