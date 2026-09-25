package com.self.profile.util

import java.time.format.DateTimeFormatter

/** 全局统一的日期展示格式（DateTimeFormatter 不可变且线程安全，可直接共享） */
object DateFormats {
    /** 完整日期，如「2026年9月25日」 */
    val FULL: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日")

    /** 短日期，如「9月25日」 */
    val SHORT: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日")
}
