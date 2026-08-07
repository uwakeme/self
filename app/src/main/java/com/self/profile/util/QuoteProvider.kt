package com.self.profile.util

import java.time.LocalDate
import kotlin.math.absoluteValue

/**
 * 每日寄语库。按日期轮换，确保同一天所有设备看到同一句。
 */
object QuoteProvider {

    data class Quote(val text: String, val author: String? = null, val subline: String? = null)

    private val quotes: List<Quote> = listOf(
        Quote("认识自己，是一切智慧的开端。", "Aristotle", "记录每一天的感受，让成长有迹可循"),
        Quote("人是万物的尺度。", "Protagoras", "今天的你，是过去所有选择的总和"),
        Quote("成为你自己。", "Nietzsche", "不必成为别人期待的样子"),
        Quote("你所拥有的每一个平凡的日常，都是别人羡慕的奇迹。", null, "珍视眼前"),
        Quote("当你不再追随时间，时间就会追随你。", null, "慢慢来，比较快"),
        Quote("人是寻求意义的生物。", "Viktor Frankl", "找到属于自己那朵玫瑰"),
        Quote("在隆冬，我终于知道，我身上有一个不可战胜的夏天。", "Albert Camus", "再难也总有温暖"),
        Quote("我们读所有的书，最终都是为了读自己。", null, "今天也读几页吧"),
        Quote("所谓理解，往往不过是误解的总和。", "村上春树", "理解别人，先理解自己"),
        Quote("一个人知道自己为什么而活，就能忍受任何一种生活。", "Nietzsche", "找到你的'为什么'"),
        Quote("温柔的人，往往是最强大的人。", null, "柔软不输力量"),
        Quote("世间所有的相遇，都是久别重逢。", null, "珍惜身边每一个")
    )

    /**
     * 按日期稳定选择一条。今天看到的，明天也看到。
     */
    fun today(): Quote {
        val today = LocalDate.now()
        val dayOfYear = today.dayOfYear
        val idx = (dayOfYear * 31 + today.year).absoluteValue % quotes.size
        return quotes[idx]
    }
}
