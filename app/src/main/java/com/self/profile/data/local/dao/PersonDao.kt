package com.self.profile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.self.profile.data.local.entity.PersonEntity
import kotlinx.coroutines.flow.Flow

/**
 * 人物（包含自己）数据访问。
 *
 * 软删除约定：`deleted_at IS NULL` 为在册数据，常规查询一律过滤软删行；
 * 回收站相关操作见「回收站」小节。
 */
@Dao
interface PersonDao {

    @Query("SELECT * FROM persons WHERE deleted_at IS NULL ORDER BY created_at DESC")
    fun observeAll(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM persons WHERE is_self = 1 AND deleted_at IS NULL LIMIT 1")
    fun observeSelf(): Flow<PersonEntity?>

    // 不过滤软删行：回收站预览 / 恢复流程需要按 id 读到已删人物
    @Query("SELECT * FROM persons WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<PersonEntity?>

    @Query("SELECT * FROM persons WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PersonEntity?

    @Query("SELECT * FROM persons WHERE relationship_type = :type AND deleted_at IS NULL ORDER BY created_at DESC")
    fun observeByRelationship(type: String): Flow<List<PersonEntity>>

    @Query(
        """
        SELECT * FROM persons
        WHERE is_self = 0 AND deleted_at IS NULL AND name LIKE '%' || :query || '%'
        ORDER BY created_at DESC
        """
    )
    fun searchByName(query: String): Flow<List<PersonEntity>>

    @Query("SELECT COUNT(*) FROM persons WHERE relationship_type = :type AND is_self = 0 AND deleted_at IS NULL")
    fun countByRelationship(type: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM persons WHERE is_self = 0 AND deleted_at IS NULL")
    fun countOthers(): Flow<Int>

    // ====== 回收站（软删除） ======

    @Query("SELECT * FROM persons WHERE deleted_at IS NOT NULL AND is_self = 0 ORDER BY deleted_at DESC")
    fun observeDeleted(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM persons WHERE deleted_at IS NOT NULL AND deleted_at <= :timestamp AND is_self = 0")
    suspend fun getDeletedBefore(timestamp: Long): List<PersonEntity>

    @Query("SELECT COUNT(*) FROM persons WHERE deleted_at IS NOT NULL")
    fun countDeleted(): Flow<Int>

    @Query("UPDATE persons SET deleted_at = :timestamp, updated_at = :timestamp WHERE id = :id AND is_self = 0")
    suspend fun softDelete(id: String, timestamp: Long)

    @Query("UPDATE persons SET deleted_at = NULL, updated_at = :timestamp WHERE id = :id")
    suspend fun restore(id: String, timestamp: Long)

    /** 彻底删除软删超过 [timestamp] 的行（is_self 永不删），返回删除数 */
    @Query("DELETE FROM persons WHERE deleted_at IS NOT NULL AND deleted_at <= :timestamp AND is_self = 0")
    suspend fun purgeDeletedBefore(timestamp: Long): Int

    // ====== 写入 ======

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(person: PersonEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(persons: List<PersonEntity>)

    @Update
    suspend fun update(person: PersonEntity)

    @Query("UPDATE persons SET last_viewed_at = :timestamp WHERE id = :id")
    suspend fun updateLastViewed(id: String, timestamp: Long)

    /** 硬删除。回收站"彻底删除"与数据导入前清空使用 */
    @Query("DELETE FROM persons WHERE id = :id AND is_self = 0")
    suspend fun deleteById(id: String)

    /** 清空所有人物（含自己，含软删行），用于"数据管理 - 清除"/导入前清空 */
    @Query("DELETE FROM persons")
    suspend fun deleteAll()

    /** 总行数（含软删行）：仅用于种子数据 isEmpty 判断 */
    @Query("SELECT COUNT(*) FROM persons")
    suspend fun count(): Int
}
