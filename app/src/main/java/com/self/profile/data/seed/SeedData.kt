package com.self.profile.data.seed

import com.self.profile.data.repository.PersonRepository
import com.self.profile.data.repository.ReminderRepository
import com.self.profile.domain.model.AppSettings
import com.self.profile.data.datastore.SettingsDataStore
import com.self.profile.domain.model.Anniversary
import com.self.profile.domain.model.Aspiration
import com.self.profile.domain.model.BodyData
import com.self.profile.domain.model.CategorizedItems
import com.self.profile.domain.model.ClosenessLevel
import com.self.profile.domain.model.CommonGround
import com.self.profile.domain.model.LifeJourneyEntry
import com.self.profile.domain.model.Note
import com.self.profile.domain.model.PersonalBackground
import com.self.profile.domain.model.Person
import com.self.profile.domain.model.Photo
import com.self.profile.domain.model.RelationshipType
import com.self.profile.domain.model.Reminder
import com.self.profile.domain.model.ReminderType
import com.self.profile.domain.model.RepeatRule
import com.self.profile.domain.model.SizeInfo
import com.self.profile.domain.model.WishItem
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 首次启动种子数据。
 *
 * 检测数据库为空时写入 1 个 self + 5 个其他人物 + 3 条提醒。
 * 后续用户可编辑、删除，不会重新触发。
 */
@Singleton
class SeedData @Inject constructor(
    private val personRepo: PersonRepository,
    private val reminderRepo: ReminderRepository,
    private val settingsStore: SettingsDataStore
) {
    suspend fun seedIfEmpty() {
        if (!personRepo.isEmpty()) return
        val now = System.currentTimeMillis()
        personRepo.upsertAll(buildSeedPersons(now))
        reminderRepo.upsertAll(buildSeedReminders(now))
    }

    private fun buildSeedPersons(now: Long): List<Person> {
        val selfId = "self-001"
        val familyMomId = "p-mom"
        val familySisterId = "p-sister"
        val friendLinId = "p-lin"
        val friendJieId = "p-jie"
        val colleagueShiId = "p-shi"

        return listOf(
            // 我自己
            Person(
                id = selfId,
                name = "小暖",
                avatarColor = "#C4785B",
                avatarInitial = "我",
                relationshipType = RelationshipType.FRIEND,  // isSelf 时无所谓
                mbti = "INFJ",
                bloodType = "A",
                closenessLevel = ClosenessLevel.CLOSE,
                isSelf = true,
                personalBackground = PersonalBackground(
                    hometown = "浙江·杭州",
                    currentCity = "上海",
                    birthday = LocalDate.of(1999, 6, 15).toEpochDay(),
                    lunarBirthday = "五月初三",
                    constellation = "双子座",
                    luckyNumbers = listOf("3", "7"),
                    university = "华东师范大学",
                    major = "汉语言文学",
                    job = "内容编辑"
                ),
                bodyData = BodyData(
                    bloodType = "A",
                    height = 162f,
                    weight = 48f,
                    shoulderWidth = 36f,
                    waist = 62f,
                    hip = 88f
                ),
                sizeInfo = SizeInfo(shoeSize = "37", topSize = "S", pantsSize = "S"),
                thingsLiked = listOf(
                    CategorizedItems("食物", listOf("火锅", "抹茶", "杨枝甘露")),
                    CategorizedItems("音乐", listOf("民谣", "轻爵士")),
                    CategorizedItems("季节", listOf("秋天", "初夏"))
                ),
                thingsDisliked = listOf(
                    CategorizedItems("食物", listOf("香菜", "苦瓜")),
                    CategorizedItems("天气", listOf("回南天", "暴晒"))
                ),
                personalityAnalysis = "偏内向但熟悉后会话多；做事追求条理；情绪敏感但不轻易表达。",
                idealType = "真诚、温和、有自己的小世界；不一定要外向但愿意主动分享生活。",
                aspirations = listOf(
                    Aspiration(UUID.randomUUID().toString(), "每年读完 20 本书", now),
                    Aspiration(UUID.randomUUID().toString(), "学一门乐器（尤克里里）", now),
                    Aspiration(UUID.randomUUID().toString(), "存够钱去一趟冰岛", now)
                ),
                wishList = listOf(
                    WishItem(UUID.randomUUID().toString(), "养一只英短", false, now),
                    WishItem(UUID.randomUUID().toString(), "学烘焙", false, now),
                    WishItem(UUID.randomUUID().toString(), "出版一本手账书", true, now)
                ),
                lifeJourney = listOf(
                    LifeJourneyEntry(UUID.randomUUID().toString(), "2017", "考入华东师大", "教育", "中文系，开始系统读文学作品"),
                    LifeJourneyEntry(UUID.randomUUID().toString(), "2019", "学校文学社社长", "教育", "组织过多场读书会"),
                    LifeJourneyEntry(UUID.randomUUID().toString(), "2021", "入职出版社", "工作", "做内容编辑，开始职场"),
                    LifeJourneyEntry(UUID.randomUUID().toString(), "2023", "搬到上海", "工作", "独立生活，结识新朋友")
                ),
                thoughtNotes = listOf(
                    Note(UUID.randomUUID().toString(), "今天整理了书架，发现自己买的书比读的多一倍。",
                        LocalDate.now().minusDays(1).toEpochDay()),
                    Note(UUID.randomUUID().toString(), "晚上和妈妈视频，她说想我了。记一下：下次主动打给她。",
                        LocalDate.now().minusDays(5).toEpochDay())
                ),
                photos = emptyList(),
                anniversaries = listOf(
                    Anniversary(UUID.randomUUID().toString(), "入职纪念日",
                        LocalDate.of(2021, 7, 1).toEpochDay(), "纪念日")
                ),
                createdAt = now,
                updatedAt = now
            ),

            // 妈妈
            Person(
                id = familyMomId,
                name = "妈妈",
                avatarColor = "#C4785B",
                avatarInitial = "妈",
                relationshipType = RelationshipType.FAMILY,
                closenessLevel = ClosenessLevel.CLOSE,
                personalBackground = PersonalBackground(
                    hometown = "浙江·杭州",
                    birthday = LocalDate.of(1968, 3, 15).toEpochDay(),
                    lunarBirthday = "二月十七",
                    constellation = "双鱼座",
                    job = "退休教师"
                ),
                thingsLiked = listOf(
                    CategorizedItems("食物", listOf("清蒸鱼", "龙井虾仁")),
                    CategorizedItems("活动", listOf("散步", "看越剧"))
                ),
                interactionNotes = listOf(
                    Note(UUID.randomUUID().toString(), "今天和妈妈视频，她说想去西湖走走。",
                        LocalDate.now().minusDays(2).toEpochDay())
                ),
                commonGrounds = listOf(
                    CommonGround(UUID.randomUUID().toString(), "都爱看越剧《红楼梦》"),
                    CommonGround(UUID.randomUUID().toString(), "都喜欢清蒸鱼胜过红烧")
                ),
                anniversaries = listOf(
                    Anniversary(UUID.randomUUID().toString(), "妈妈的生日",
                        LocalDate.of(1968, 3, 15).toEpochDay(), "生日")
                ),
                createdAt = now,
                updatedAt = now
            ),

            // 姐姐
            Person(
                id = familySisterId,
                name = "姐姐",
                avatarColor = "#D99B82",
                avatarInitial = "姐",
                relationshipType = RelationshipType.FAMILY,
                closenessLevel = ClosenessLevel.CLOSE,
                personalBackground = PersonalBackground(
                    currentCity = "杭州",
                    birthday = LocalDate.of(1995, 11, 23).toEpochDay(),
                    job = "产品经理"
                ),
                commonGrounds = listOf(
                    CommonGround(UUID.randomUUID().toString(), "都是 INFJ"),
                    CommonGround(UUID.randomUUID().toString(), "都喜欢手账")
                ),
                anniversaries = listOf(
                    Anniversary(UUID.randomUUID().toString(), "姐姐生日",
                        LocalDate.of(1995, 11, 23).toEpochDay(), "生日")
                ),
                createdAt = now,
                updatedAt = now
            ),

            // 小林
            Person(
                id = friendLinId,
                name = "小林",
                avatarColor = "#6B8EAE",
                avatarInitial = "林",
                relationshipType = RelationshipType.FRIEND,
                mbti = "ENFP",
                closenessLevel = ClosenessLevel.MEDIUM,
                personalBackground = PersonalBackground(
                    currentCity = "上海",
                    birthday = LocalDate.of(1998, 6, 20).toEpochDay(),
                    university = "上海交通大学",
                    job = "UI 设计师"
                ),
                interactionNotes = listOf(
                    Note(UUID.randomUUID().toString(), "约了周末去安福路看展。",
                        LocalDate.now().minusDays(1).toEpochDay())
                ),
                commonGrounds = listOf(
                    CommonGround(UUID.randomUUID().toString(), "都喜欢逛独立书店")
                ),
                anniversaries = listOf(
                    Anniversary(UUID.randomUUID().toString(), "认识纪念日",
                        LocalDate.of(2022, 6, 20).toEpochDay(), "纪念日")
                ),
                createdAt = now,
                updatedAt = now
            ),

            // 阿杰
            Person(
                id = friendJieId,
                name = "阿杰",
                avatarColor = "#6B8EAE",
                avatarInitial = "杰",
                relationshipType = RelationshipType.FRIEND,
                mbti = "ISTP",
                closenessLevel = ClosenessLevel.MEDIUM,
                personalBackground = PersonalBackground(
                    job = "摄影师"
                ),
                anniversaries = emptyList(),
                createdAt = now,
                updatedAt = now
            ),

            // 张老师
            Person(
                id = colleagueShiId,
                name = "张老师",
                avatarColor = "#6B9E78",
                avatarInitial = "师",
                relationshipType = RelationshipType.COLLEAGUE,
                closenessLevel = ClosenessLevel.MEDIUM,
                personalBackground = PersonalBackground(
                    job = "高级编辑"
                ),
                commonGrounds = listOf(
                    CommonGround(UUID.randomUUID().toString(), "都喜欢纪德")
                ),
                createdAt = now,
                updatedAt = now
            )
        )
    }

    private fun buildSeedReminders(now: Long): List<Reminder> = listOf(
        Reminder(
            id = "r-1",
            personId = "p-mom",
            type = ReminderType.BIRTHDAY,
            title = "妈妈的生日",
            date = LocalDate.of(LocalDate.now().year, 3, 15).toEpochDay(),
            advanceDays = 3,
            reminderTime = "08:00",
            repeat = RepeatRule.YEARLY,
            enabled = true,
            createdAt = now,
            updatedAt = now
        ),
        Reminder(
            id = "r-2",
            personId = "p-lin",
            type = ReminderType.ANNIVERSARY,
            title = "认识纪念日",
            date = LocalDate.of(LocalDate.now().year, 6, 20).toEpochDay(),
            advanceDays = 1,
            reminderTime = "08:00",
            repeat = RepeatRule.YEARLY,
            enabled = true,
            createdAt = now,
            updatedAt = now
        ),
        Reminder(
            id = "r-3",
            personId = "p-sister",
            type = ReminderType.BIRTHDAY,
            title = "姐姐生日",
            date = LocalDate.of(LocalDate.now().year, 11, 23).toEpochDay(),
            advanceDays = 7,
            reminderTime = "08:00",
            repeat = RepeatRule.YEARLY,
            enabled = true,
            createdAt = now,
            updatedAt = now
        )
    )
}
