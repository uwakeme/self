package com.self.profile.ui.search

import com.self.profile.domain.model.Note
import com.self.profile.domain.model.Person
import com.self.profile.domain.model.RelationshipType
import com.self.profile.domain.model.WishItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GlobalSearchTest {

    private fun person(
        id: String,
        name: String,
        city: String? = null,
        tags: List<String> = emptyList(),
        notes: List<Note> = emptyList(),
        wishes: List<WishItem> = emptyList()
    ) = Person(
        id = id,
        name = name,
        avatarColor = "#C4785B",
        avatarInitial = name.take(1),
        relationshipType = RelationshipType.FRIEND,
        isSelf = false,
        personalBackground = com.self.profile.domain.model.PersonalBackground(currentCity = city),
        interactionNotes = notes,
        wishList = wishes,
        tags = tags,
        createdAt = 0L,
        updatedAt = 0L
    )

    @Test
    fun `空查询返回空`() {
        assertTrue(GlobalSearchViewModel.searchAll(listOf(person("1", "小林")), "").isEmpty())
        assertTrue(GlobalSearchViewModel.searchAll(listOf(person("1", "小林")), "  ").isEmpty())
    }

    @Test
    fun `按姓名命中 PERSON 类别`() {
        val hits = GlobalSearchViewModel.searchAll(listOf(person("1", "小林"), person("2", "阿杰")), "林")
        assertEquals(1, hits.size)
        assertEquals(SearchHitKind.PERSON, hits[0].kind)
        assertEquals("小林", hits[0].title)
    }

    @Test
    fun `按城市与标签命中基础信息`() {
        val people = listOf(person("1", "小林", city = "上海"), person("2", "阿杰", tags = listOf("球友")))
        assertEquals("小林", GlobalSearchViewModel.searchAll(people, "上海").single().person.name)
        assertEquals("阿杰", GlobalSearchViewModel.searchAll(people, "球友").single().person.name)
    }

    @Test
    fun `笔记与愿望命中并带人物上下文`() {
        val p = person(
            "1", "妈妈",
            notes = listOf(Note("n1", "周末去西湖走走", 0)),
            wishes = listOf(WishItem("w1", "学一门乐器", false, 0))
        )
        val noteHits = GlobalSearchViewModel.searchAll(listOf(p), "西湖")
        assertEquals(SearchHitKind.NOTE, noteHits.single().kind)
        assertEquals("妈妈", noteHits.single().person.name)

        val wishHits = GlobalSearchViewModel.searchAll(listOf(p), "乐器")
        assertEquals(SearchHitKind.WISH, wishHits.single().kind)
        assertTrue(wishHits.single().snippet.contains("乐器"))
    }

    @Test
    fun `人物命中排在内容命中之前`() {
        val p = person("1", "张三", notes = listOf(Note("n1", "提到李四", 0)))
        val other = person("2", "李四")
        val hits = GlobalSearchViewModel.searchAll(listOf(p, other), "李四")
        assertEquals(SearchHitKind.PERSON, hits.first().kind)
        assertEquals("李四", hits.first().person.name)
    }
}
