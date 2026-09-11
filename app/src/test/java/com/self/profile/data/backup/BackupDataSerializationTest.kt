package com.self.profile.data.backup

import com.self.profile.data.local.AppJson
import com.self.profile.domain.model.Person
import com.self.profile.domain.model.Photo
import com.self.profile.domain.model.RelationshipType
import com.self.profile.domain.model.Reminder
import com.self.profile.domain.model.ReminderType
import com.self.profile.domain.model.RepeatRule
import kotlinx.serialization.builtins.ListSerializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BackupDataSerializationTest {

    private fun person() = Person(
        id = "p1",
        name = "妈妈",
        avatarColor = "#C4785B",
        avatarInitial = "妈",
        relationshipType = RelationshipType.FAMILY,
        isSelf = false,
        photos = listOf(Photo("ph1", "/data/user/0/app/files/photos/a.jpg", "旧照", 1L)),
        tags = listOf("家人", "常用"),
        deletedAt = null,
        createdAt = 1L,
        updatedAt = 2L
    )

    private fun reminder() = Reminder(
        id = "r1",
        personId = "p1",
        type = ReminderType.BIRTHDAY,
        title = "妈妈的生日",
        date = 100L,
        repeat = RepeatRule.YEARLY,
        isLunar = true,
        createdAt = 1L,
        updatedAt = 2L
    )

    @Test
    fun `v2 备份 round-trip 保留新字段`() {
        val data = BackupData(
            version = 2,
            exportedAt = 42L,
            people = listOf(person()),
            reminders = listOf(reminder())
        )
        val json = AppJson.encodeToString(BackupData.serializer(), data)
        val decoded = AppJson.decodeFromString(BackupData.serializer(), json)

        assertEquals(2, decoded.version)
        val p = decoded.people.single()
        assertEquals(listOf("家人", "常用"), p.tags)
        assertNull(p.deletedAt)
        assertEquals("/data/user/0/app/files/photos/a.jpg", p.photos.single().url)
        assertTrue(decoded.reminders.single().isLunar)
    }

    @Test
    fun `v1 旧备份含已移除字段仍可解析`() {
        // v1 时代 AppSettings 带 dataExportFormat；ignoreUnknownKeys 必须容忍，
        // 且新字段（tags/is_lunar/deletedAt）缺失时回退默认值
        val legacyJson = """
            {
              "version": 1,
              "exportedAt": 1000,
              "people": [
                {
                  "id": "p1", "name": "妈妈", "avatarColor": "#C4785B", "avatarInitial": "妈",
                  "relationshipType": "FAMILY", "closenessLevel": "CLOSE", "isSelf": false,
                  "createdAt": 1, "updatedAt": 2
                }
              ],
              "reminders": [],
              "settings": { "dataExportFormat": "json", "userNickname": "小暖" }
            }
        """.trimIndent()

        val decoded = AppJson.decodeFromString(BackupData.serializer(), legacyJson)
        assertEquals(1, decoded.version)
        assertEquals("妈妈", decoded.people.single().name)
        assertTrue(decoded.people.single().tags.isEmpty())
        assertEquals("小暖", decoded.settings.userNickname)
    }

    @Test
    fun `Person JSON 里照片路径列表独立可用`() {
        // 导入器依赖对 Person.photos 的直接编解码（zip 内路径重写）
        val json = AppJson.encodeToString(
            ListSerializer(Photo.serializer()),
            listOf(Photo("ph1", "photos/a.jpg", null, 0L))
        )
        val decoded = AppJson.decodeFromString(ListSerializer(Photo.serializer()), json)
        assertEquals("photos/a.jpg", decoded.single().url)
    }
}
