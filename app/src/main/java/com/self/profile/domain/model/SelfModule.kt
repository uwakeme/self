package com.self.profile.domain.model

/**
 * 自我档案的 13 个可排序模块。
 *
 * 排序在 [AppSettings.selfProfileModuleOrder] 里持久化（List<Int>，存的是 ordinal + 1）。
 */
enum class SelfModule(val title: String) {
    BACKGROUND("个人背景"),
    BODY("身体数据"),
    SIZE("尺码信息"),
    LIKED("最喜欢的事物"),
    DISLIKED("最不喜欢的事物"),
    PERSONALITY("性格分析"),
    IDEAL("理想型画像"),
    ASPIRATIONS("憧憬与目标"),
    WISHES("愿望清单"),
    LIFE_JOURNEY("人生轨迹"),
    NOTES("随想笔记"),
    PHOTOS("照片墙"),
    ANNIVERSARIES("纪念日");

    companion object {
        val defaultOrder: List<Int> = values().map { it.ordinal + 1 }
    }
}
