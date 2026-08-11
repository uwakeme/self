package com.self.profile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.self.profile.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

/**
 * 提醒数据访问。
 */
@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY date ASC")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE person_id = :personId ORDER BY date ASC")
    fun observeByPerson(personId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE enabled = 1 ORDER BY date ASC")
    fun observeEnabled(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ReminderEntity?>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ReminderEntity?

    @Query("SELECT COUNT(*) FROM reminders")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM reminders WHERE enabled = 1")
    fun observeEnabledCount(): Flow<Int>

    @Query(
        """
        SELECT * FROM reminders
        WHERE enabled = 1 AND date BETWEEN :fromDate AND :toDate
        ORDER BY date ASC
        """
    )
    fun observeUpcoming(fromDate: Long, toDate: Long): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reminders: List<ReminderEntity>)

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("UPDATE reminders SET enabled = :enabled, updated_at = :updatedAt WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean, updatedAt: Long)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: String)

    /** 清空所有提醒，用于"数据管理 - 清除"流程 */
    @Query("DELETE FROM reminders")
    suspend fun deleteAll()
}
