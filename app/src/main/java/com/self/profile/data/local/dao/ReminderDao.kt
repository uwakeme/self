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
 *
 * 软删除约定：`deleted_at IS NULL` 为在册数据；列表 / 调度相关查询一律过滤软删行。
 */
@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE deleted_at IS NULL ORDER BY date ASC")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE person_id = :personId AND deleted_at IS NULL ORDER BY date ASC")
    fun observeByPerson(personId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE enabled = 1 AND deleted_at IS NULL ORDER BY date ASC")
    fun observeEnabled(): Flow<List<ReminderEntity>>

    // 不过滤软删行：Worker 触发时要靠 deletedAt 判断是否已被删入回收站
    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ReminderEntity?>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ReminderEntity?

    @Query("SELECT COUNT(*) FROM reminders WHERE deleted_at IS NULL")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM reminders WHERE enabled = 1 AND deleted_at IS NULL")
    fun observeEnabledCount(): Flow<Int>

    @Query(
        """
        SELECT * FROM reminders
        WHERE enabled = 1 AND deleted_at IS NULL AND date BETWEEN :fromDate AND :toDate
        ORDER BY date ASC
        """
    )
    fun observeUpcoming(fromDate: Long, toDate: Long): Flow<List<ReminderEntity>>

    // ====== 回收站（软删除） ======

    @Query("SELECT * FROM reminders WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    fun observeDeleted(): Flow<List<ReminderEntity>>

    @Query("SELECT COUNT(*) FROM reminders WHERE deleted_at IS NOT NULL")
    fun countDeleted(): Flow<Int>

    @Query("UPDATE reminders SET deleted_at = :timestamp, updated_at = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long)

    @Query(
        """
        UPDATE reminders SET deleted_at = :timestamp, updated_at = :timestamp
        WHERE person_id = :personId AND deleted_at IS NULL
        """
    )
    suspend fun softDeleteByPerson(personId: String, timestamp: Long)

    @Query("UPDATE reminders SET deleted_at = NULL, updated_at = :timestamp WHERE id = :id")
    suspend fun restore(id: String, timestamp: Long)

    /** 软删行彻底清除，返回删除数 */
    @Query("DELETE FROM reminders WHERE deleted_at IS NOT NULL AND deleted_at <= :timestamp")
    suspend fun purgeDeletedBefore(timestamp: Long): Int

    /** 人物关联的全部提醒（含软删行）：随人物恢复 / 彻底删除时使用 */
    @Query("SELECT * FROM reminders WHERE person_id = :personId")
    suspend fun getByPersonAnyState(personId: String): List<ReminderEntity>

    // ====== 写入 ======

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reminders: List<ReminderEntity>)

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("UPDATE reminders SET enabled = :enabled, updated_at = :updatedAt WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean, updatedAt: Long)

    /** 硬删除。回收站"彻底删除"与数据清除使用 */
    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: String)

    /** 清空所有提醒（含软删行），用于"数据管理 - 清除"/导入前清空 */
    @Query("DELETE FROM reminders")
    suspend fun deleteAll()
}
