package com.self.profile.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.self.profile.data.local.dao.PersonDao
import com.self.profile.data.local.entity.PersonEntity
import com.self.profile.domain.model.RelationshipType
import com.self.profile.testutil.createInMemoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 人物软删除语义测试。
 *
 * 约定：`persons.deleted_at IS NULL` 为在册数据，常规查询一律过滤软删行；
 * 回收站相关查询保留软删行。这是纯 SQL 层的契约，用 in-memory Room 真跑一遍
 * 比 mock 更能说明问题（mock 只能验「调了哪个方法」，验不了 SQL 写没写对）。
 *
 * 注意这里是 AndroidJUnit4 / JUnit 4，跟 `app/src/test/` 下的 JUnit 5 单测是两套体系。
 */
@RunWith(AndroidJUnit4::class)
class PersonSoftDeleteTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: PersonDao

    /** 固定时间戳，避免依赖当前时间导致断言漂移 */
    private val now = 1_760_000_000_000L

    @Before
    fun setUp() {
        db = createInMemoryDatabase()
        dao = db.personDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun person(
        id: String,
        createdAt: Long,
        isSelf: Boolean = false
    ) = PersonEntity(
        id = id,
        name = "人物-$id",
        avatarColor = "#C4785B",
        avatarInitial = "人",
        relationshipType = RelationshipType.FRIEND.label,
        isSelf = isSelf,
        createdAt = createdAt,
        updatedAt = createdAt
    )

    /** 核心红线：软删后常规查询看不到这条 */
    @Test
    fun 软删后常规查询过滤掉该行() = runBlocking {
        dao.insertAll(listOf(person("p-keep", 2), person("p-del", 1)))
        dao.softDelete("p-del", now)

        assertEquals(
            listOf("p-keep"),
            dao.observeAll().first().map { it.id }
        )
    }

    /** 对照组：同一条数据在回收站查询里必须留着 */
    @Test
    fun 回收站查询保留软删行() = runBlocking {
        dao.insertAll(listOf(person("p-keep", 2), person("p-del", 1)))
        dao.softDelete("p-del", now)

        assertEquals(
            listOf("p-del"),
            dao.observeDeleted().first().map { it.id }
        )
    }

    /**
     * 总行数含软删行 —— 种子数据的 `isEmpty()` 判据就是它。
     * 这条钉住「软删不等于清空」，否则用户删光所有人物后种子会复活。
     */
    @Test
    fun 软删不影响总行数但不再计入在册人数() = runBlocking {
        dao.insertAll(listOf(person("p-del", 1)))
        dao.softDelete("p-del", now)

        assertEquals(1, dao.count())
        assertEquals(0, dao.countOthers().first())
        assertEquals(1, dao.countDeleted().first())
    }

    /** 回收站预览要按 id 读到已删人物，所以按 id 查询不过滤软删行 */
    @Test
    fun 按id查询不过滤软删行() = runBlocking {
        dao.insert(person("p-del", 1))
        dao.softDelete("p-del", now)

        assertNotNull(dao.getById("p-del"))
    }

    /** 恢复后重新出现在常规查询里，且回收站里不再有它 */
    @Test
    fun 恢复后重新出现在常规查询里() = runBlocking {
        dao.insertAll(listOf(person("p-keep", 2), person("p-del", 1)))
        dao.softDelete("p-del", now)
        dao.restore("p-del", now + 1)

        assertEquals(
            setOf("p-keep", "p-del"),
            dao.observeAll().first().map { it.id }.toSet()
        )
        assertEquals(emptyList<String>(), dao.observeDeleted().first().map { it.id })
    }

    /** 自己不能被软删，否则 Self 页会空掉 */
    @Test
    fun 自我档案不可软删() = runBlocking {
        dao.insert(person("self-001", 1, isSelf = true))
        dao.softDelete("self-001", now)

        assertEquals(
            listOf("self-001"),
            dao.observeAll().first().map { it.id }
        )
    }

    /** 彻底删除只清理超过保留期的软删行，在册行不受影响 */
    @Test
    fun 彻底删除只清理超过保留期的软删行() = runBlocking {
        dao.insertAll(listOf(person("p-old", 1), person("p-new", 2)))
        dao.softDelete("p-old", now)
        dao.softDelete("p-new", now + 10)

        val purged = dao.purgeDeletedBefore(now + 5)

        assertEquals(1, purged)
        assertEquals(listOf("p-new"), dao.observeDeleted().first().map { it.id })
        assertEquals(emptyList<String>(), dao.observeAll().first().map { it.id })
    }
}