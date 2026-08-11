package com.self.profile.data.repository

import com.self.profile.data.local.AppJson
import com.self.profile.data.local.dao.PersonDao
import com.self.profile.data.local.entity.PersonalBackgroundEntity
import com.self.profile.data.local.entity.PersonEntity
import com.self.profile.domain.model.Aspiration
import com.self.profile.domain.model.Anniversary
import com.self.profile.domain.model.BodyData
import com.self.profile.domain.model.CategorizedItems
import com.self.profile.domain.model.ClosenessLevel
import com.self.profile.domain.model.CommonGround
import com.self.profile.domain.model.LifeJourneyEntry
import com.self.profile.domain.model.Note
import com.self.profile.domain.model.Person
import com.self.profile.domain.model.PersonalBackground
import com.self.profile.domain.model.Photo
import com.self.profile.domain.model.RelationshipType
import com.self.profile.domain.model.SizeInfo
import com.self.profile.domain.model.WishItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 人物仓库。
 *
 * - 提供 Flow<List<Person>> 等 UI 直接消费的领域模型
 * - 在内部做 entity ↔ domain 转换（嵌套 JSON 解析）
 */
@Singleton
class PersonRepository @Inject constructor(
    private val dao: PersonDao
) {

    fun observeAll(): Flow<List<Person>> = dao.observeAll().map { it.map(::toDomain) }

    fun observeSelf(): Flow<Person?> = dao.observeSelf().map { it?.let(::toDomain) }

    fun observeById(id: String): Flow<Person?> = dao.observeById(id).map { it?.let(::toDomain) }

    fun observeByRelationship(type: RelationshipType): Flow<List<Person>> =
        dao.observeByRelationship(type.label).map { it.map(::toDomain) }

    fun searchByName(query: String): Flow<List<Person>> =
        dao.searchByName(query).map { it.map(::toDomain) }

    fun countByRelationship(type: RelationshipType): Flow<Int> =
        dao.countByRelationship(type.label)

    fun countOthers(): Flow<Int> = dao.countOthers()

    suspend fun upsert(person: Person) = dao.insert(toEntity(person))

    suspend fun upsertAll(persons: List<Person>) = dao.insertAll(persons.map(::toEntity))

    suspend fun updateLastViewed(id: String, timestamp: Long) =
        dao.updateLastViewed(id, timestamp)

    suspend fun delete(id: String) = dao.deleteById(id)

    /** 清空所有人物（含自己）。供"数据管理 - 清除"使用。 */
    suspend fun deleteAll() = dao.deleteAll()

    suspend fun isEmpty(): Boolean = dao.count() == 0

    // ====== 转换层 ======

    private fun toDomain(e: PersonEntity): Person = Person(
        id = e.id,
        name = e.name,
        avatarColor = e.avatarColor,
        avatarInitial = e.avatarInitial,
        relationshipType = RelationshipType.fromLabel(e.relationshipType),
        mbti = e.mbti,
        bloodType = e.bloodType,
        closenessLevel = ClosenessLevel.values().firstOrNull { it.label == e.closenessLevel }
            ?: ClosenessLevel.MEDIUM,
        isSelf = e.isSelf,
        personalBackground = e.personalBackground.toDomain(),
        bodyData = e.bodyData.toDomain(),
        sizeInfo = e.sizeInfo.toDomain(),
        thingsLiked = decodeList(e.thingsLikedJson, CategorizedItems.serializer()),
        thingsDisliked = decodeList(e.thingsDislikedJson, CategorizedItems.serializer()),
        personalityAnalysis = e.personalityAnalysis,
        idealType = e.idealType,
        aspirations = decodeList(e.aspirationsJson, Aspiration.serializer()),
        wishList = decodeList(e.wishListJson, WishItem.serializer()),
        lifeJourney = decodeList(e.lifeJourneyJson, LifeJourneyEntry.serializer()),
        thoughtNotes = decodeList(e.thoughtNotesJson, Note.serializer()),
        interactionNotes = decodeList(e.interactionNotesJson, Note.serializer()),
        commonGrounds = decodeList(e.commonGroundsJson, CommonGround.serializer()),
        photos = decodeList(e.photosJson, Photo.serializer()),
        anniversaries = decodeList(e.anniversariesJson, Anniversary.serializer()),
        createdAt = e.createdAt,
        updatedAt = e.updatedAt,
        lastViewedAt = e.lastViewedAt
    )

    private fun toEntity(p: Person): PersonEntity = PersonEntity(
        id = p.id,
        name = p.name,
        avatarColor = p.avatarColor,
        avatarInitial = p.avatarInitial,
        relationshipType = p.relationshipType.label,
        mbti = p.mbti,
        bloodType = p.bloodType,
        closenessLevel = p.closenessLevel.label,
        isSelf = p.isSelf,
        personalBackground = p.personalBackground.toEntity(),
        bodyData = p.bodyData.toEntity(),
        sizeInfo = p.sizeInfo.toEntity(),
        thingsLikedJson = encodeList(p.thingsLiked, CategorizedItems.serializer()),
        thingsDislikedJson = encodeList(p.thingsDisliked, CategorizedItems.serializer()),
        personalityAnalysis = p.personalityAnalysis,
        idealType = p.idealType,
        aspirationsJson = encodeList(p.aspirations, Aspiration.serializer()),
        wishListJson = encodeList(p.wishList, WishItem.serializer()),
        lifeJourneyJson = encodeList(p.lifeJourney, LifeJourneyEntry.serializer()),
        thoughtNotesJson = encodeList(p.thoughtNotes, Note.serializer()),
        interactionNotesJson = encodeList(p.interactionNotes, Note.serializer()),
        commonGroundsJson = encodeList(p.commonGrounds, CommonGround.serializer()),
        photosJson = encodeList(p.photos, Photo.serializer()),
        anniversariesJson = encodeList(p.anniversaries, Anniversary.serializer()),
        createdAt = p.createdAt,
        updatedAt = p.updatedAt,
        lastViewedAt = p.lastViewedAt
    )

    private fun PersonalBackgroundEntity?.toDomain(): PersonalBackground =
        this?.let {
            PersonalBackground(
                hometown = it.hometown,
                currentCity = it.currentCity,
                birthday = it.birthday,
                lunarBirthday = it.lunarBirthday,
                constellation = it.constellation,
                luckyNumbers = it.luckyNumbers
                    ?.split(",")?.map(String::trim)?.filter(String::isNotBlank) ?: emptyList(),
                university = it.university,
                major = it.major,
                job = it.job
            )
        } ?: PersonalBackground()

    private fun PersonalBackground.toEntity(): PersonalBackgroundEntity =
        PersonalBackgroundEntity(
            hometown = hometown,
            currentCity = currentCity,
            birthday = birthday,
            lunarBirthday = lunarBirthday,
            constellation = constellation,
            luckyNumbers = luckyNumbers.joinToString(",").ifBlank { null },
            university = university,
            major = major,
            job = job
        )

    private fun com.self.profile.data.local.entity.BodyDataEntity?.toDomain(): BodyData =
        this?.let {
            BodyData(
                bloodType = it.bloodType,
                height = it.height,
                weight = it.weight,
                shoulderWidth = it.shoulderWidth,
                armSpan = it.armSpan,
                chest = it.chest,
                waist = it.waist,
                hip = it.hip,
                armGirth = it.armGirth
            )
        } ?: BodyData()

    private fun BodyData.toEntity(): com.self.profile.data.local.entity.BodyDataEntity =
        com.self.profile.data.local.entity.BodyDataEntity(
            bloodType = bloodType,
            height = height,
            weight = weight,
            shoulderWidth = shoulderWidth,
            armSpan = armSpan,
            chest = chest,
            waist = waist,
            hip = hip,
            armGirth = armGirth
        )

    private fun com.self.profile.data.local.entity.SizeInfoEntity?.toDomain(): SizeInfo =
        this?.let { SizeInfo(it.shoeSize, it.topSize, it.pantsSize, it.skirtSize) } ?: SizeInfo()

    private fun SizeInfo.toEntity(): com.self.profile.data.local.entity.SizeInfoEntity =
        com.self.profile.data.local.entity.SizeInfoEntity(shoeSize, topSize, pantsSize, skirtSize)

    // ====== JSON 工具 ======

    private inline fun <reified T> decodeList(
        json: String,
        ser: kotlinx.serialization.KSerializer<T>
    ): List<T> =
        try {
            AppJson.decodeFromString(ListSerializer(ser), json)
        } catch (_: Exception) {
            emptyList()
        }

    private inline fun <reified T> encodeList(
        list: List<T>,
        ser: kotlinx.serialization.KSerializer<T>
    ): String =
        try {
            AppJson.encodeToString(ListSerializer(ser), list)
        } catch (_: Exception) {
            "[]"
        }
}
