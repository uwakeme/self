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
 */
@Dao
interface PersonDao {

    @Query("SELECT * FROM persons ORDER BY created_at DESC")
    fun observeAll(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM persons WHERE is_self = 1 LIMIT 1")
    fun observeSelf(): Flow<PersonEntity?>

    @Query("SELECT * FROM persons WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<PersonEntity?>

    @Query("SELECT * FROM persons WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PersonEntity?

    @Query("SELECT * FROM persons WHERE relationship_type = :type ORDER BY created_at DESC")
    fun observeByRelationship(type: String): Flow<List<PersonEntity>>

    @Query(
        """
        SELECT * FROM persons
        WHERE is_self = 0 AND name LIKE '%' || :query || '%'
        ORDER BY created_at DESC
        """
    )
    fun searchByName(query: String): Flow<List<PersonEntity>>

    @Query("SELECT COUNT(*) FROM persons WHERE relationship_type = :type AND is_self = 0")
    fun countByRelationship(type: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM persons WHERE is_self = 0")
    fun countOthers(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(person: PersonEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(persons: List<PersonEntity>)

    @Update
    suspend fun update(person: PersonEntity)

    @Query("UPDATE persons SET last_viewed_at = :timestamp WHERE id = :id")
    suspend fun updateLastViewed(id: String, timestamp: Long)

    @Query("DELETE FROM persons WHERE id = :id AND is_self = 0")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM persons")
    suspend fun count(): Int
}
