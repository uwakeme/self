package com.self.profile.domain.model

import kotlinx.serialization.Serializable

/**
 * 人物领域模型（对应 [com.self.profile.data.local.entity.PersonEntity]，但面向 UI / 业务层）。
 *
 * 嵌套数组字段用 @Serializable data class，Repository 负责 entity ↔ domain 的 JSON 转换。
 */
@Serializable
data class Person(
    val id: String,
    val name: String,
    val avatarColor: String,
    val avatarInitial: String,
    val relationshipType: RelationshipType,
    val mbti: String? = null,
    val bloodType: String? = null,
    val isSelf: Boolean = false,
    val personalBackground: PersonalBackground = PersonalBackground(),
    val bodyData: BodyData = BodyData(),
    val sizeInfo: SizeInfo = SizeInfo(),
    val thingsLiked: List<CategorizedItems> = emptyList(),
    val thingsDisliked: List<CategorizedItems> = emptyList(),
    val personalityAnalysis: String? = null,
    val idealType: String? = null,
    val aspirations: List<Aspiration> = emptyList(),
    val wishList: List<WishItem> = emptyList(),
    val lifeJourney: List<LifeJourneyEntry> = emptyList(),
    val thoughtNotes: List<Note> = emptyList(),            // 仅 isSelf
    val interactionNotes: List<Note> = emptyList(),        // 仅 !isSelf
    val commonGrounds: List<CommonGround> = emptyList(),   // 仅 !isSelf
    val photos: List<Photo> = emptyList(),
    val anniversaries: List<Anniversary> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long,
    val lastViewedAt: Long? = null,
    val tags: List<String> = emptyList(),
    val deletedAt: Long? = null
)

@Serializable
enum class RelationshipType(val label: String) {
    FAMILY("家人"),
    FRIEND("朋友"),
    COLLEAGUE("同事"),
    CLASSMATE("同学");

    companion object {
        fun fromLabel(label: String): RelationshipType =
            values().firstOrNull { it.label == label } ?: FRIEND
    }
}

@Serializable
data class PersonalBackground(
    val hometown: String? = null,
    val currentCity: String? = null,
    val birthday: Long? = null,            // epoch day
    val lunarBirthday: String? = null,
    val constellation: String? = null,
    val luckyNumbers: List<String> = emptyList(),
    val university: String? = null,
    val major: String? = null,
    val job: String? = null
)

@Serializable
data class BodyData(
    val bloodType: String? = null,
    val height: Float? = null,
    val weight: Float? = null,
    val shoulderWidth: Float? = null,
    val armSpan: Float? = null,
    val chest: Float? = null,
    val waist: Float? = null,
    val hip: Float? = null,
    val armGirth: Float? = null
)

@Serializable
data class SizeInfo(
    val shoeSize: String? = null,
    val topSize: String? = null,
    val pantsSize: String? = null,
    val skirtSize: String? = null
)

@Serializable
data class CategorizedItems(
    val category: String,
    val items: List<String> = emptyList()
)

@Serializable
data class Aspiration(
    val id: String,
    val content: String,
    val createdAt: Long
)

@Serializable
data class WishItem(
    val id: String,
    val content: String,
    val completed: Boolean = false,
    val createdAt: Long
)

@Serializable
data class LifeJourneyEntry(
    val id: String,
    val year: String,
    val title: String,
    val type: String,                       // 教育/工作
    val description: String
)

@Serializable
data class Note(
    val id: String,
    val content: String,
    val date: Long                          // epoch day
)

@Serializable
data class CommonGround(
    val id: String,
    val content: String
)

@Serializable
data class Photo(
    val id: String,
    val url: String,
    val description: String? = null,
    val createdAt: Long
)

@Serializable
data class Anniversary(
    val id: String,
    val title: String,
    val date: Long,                         // epoch day
    val type: String = "纪念日"
)
