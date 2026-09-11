package com.self.profile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 人物实体（含"我"自己）。
 *
 * 嵌套字段用 [Embedded] 拆到子结构类；List/嵌套对象用 JSON 字符串存储（见 [JsonConverters]）。
 */
@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "avatar_color")
    val avatarColor: String,                              // #RRGGBB

    @ColumnInfo(name = "avatar_initial")
    val avatarInitial: String,                            // 姓氏首字

    @ColumnInfo(name = "relationship_type")
    val relationshipType: String,                         // 家人/朋友/同事/同学

    @ColumnInfo(name = "mbti")
    val mbti: String? = null,

    @ColumnInfo(name = "blood_type")
    val bloodType: String? = null,

    @ColumnInfo(name = "is_self")
    val isSelf: Boolean,

    // 个人背景
    @Embedded(prefix = "bg_")
    val personalBackground: PersonalBackgroundEntity? = null,

    // 身体数据
    @Embedded(prefix = "body_")
    val bodyData: BodyDataEntity? = null,

    // 尺码
    @Embedded(prefix = "size_")
    val sizeInfo: SizeInfoEntity? = null,

    // 嵌套数组（JSON 字符串）
    @ColumnInfo(name = "things_liked_json")
    val thingsLikedJson: String = "[]",

    @ColumnInfo(name = "things_disliked_json")
    val thingsDislikedJson: String = "[]",

    @ColumnInfo(name = "personality_analysis")
    val personalityAnalysis: String? = null,

    @ColumnInfo(name = "ideal_type")
    val idealType: String? = null,

    @ColumnInfo(name = "aspirations_json")
    val aspirationsJson: String = "[]",

    @ColumnInfo(name = "wish_list_json")
    val wishListJson: String = "[]",

    @ColumnInfo(name = "life_journey_json")
    val lifeJourneyJson: String = "[]",

    /** 仅 isSelf=true 使用 */
    @ColumnInfo(name = "thought_notes_json")
    val thoughtNotesJson: String = "[]",

    /** 仅 isSelf=false 使用 */
    @ColumnInfo(name = "interaction_notes_json")
    val interactionNotesJson: String = "[]",

    /** 仅 isSelf=false 使用 */
    @ColumnInfo(name = "common_grounds_json")
    val commonGroundsJson: String = "[]",

    @ColumnInfo(name = "photos_json")
    val photosJson: String = "[]",

    @ColumnInfo(name = "anniversaries_json")
    val anniversariesJson: String = "[]",

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "last_viewed_at")
    val lastViewedAt: Long? = null,

    /** 软删除时间（epoch millis）；null = 未删除。回收站基于本列，30 天后由 Janitor 彻底清除。 */
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,

    /** 自定义标签 JSON 数组（如 ["球友","书友"]），用于图鉴自定义分组筛选 */
    @ColumnInfo(name = "tags_json", defaultValue = "[]")
    val tagsJson: String = "[]"
)

data class PersonalBackgroundEntity(
    val hometown: String? = null,
    val currentCity: String? = null,
    val birthday: Long? = null,                           // epoch day
    val lunarBirthday: String? = null,
    val constellation: String? = null,
    val luckyNumbers: String? = null,                    // 逗号分隔
    val university: String? = null,
    val major: String? = null,
    val job: String? = null
)

data class BodyDataEntity(
    val bloodType: String? = null,
    val height: Float? = null,                            // cm
    val weight: Float? = null,                            // kg
    val shoulderWidth: Float? = null,
    val armSpan: Float? = null,
    val chest: Float? = null,
    val waist: Float? = null,
    val hip: Float? = null,
    val armGirth: Float? = null
)

data class SizeInfoEntity(
    val shoeSize: String? = null,
    val topSize: String? = null,
    val pantsSize: String? = null,
    val skirtSize: String? = null
)
