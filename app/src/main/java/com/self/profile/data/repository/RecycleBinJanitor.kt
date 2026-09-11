package com.self.profile.data.repository

import com.self.profile.util.PhotoStorage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 回收站清理器：启动时把软删超过保留期的人物 / 提醒彻底删除，
 * 人物照片文件一并清除（防孤儿文件占空间）。
 */
@Singleton
class RecycleBinJanitor @Inject constructor(
    private val personRepo: PersonRepository,
    private val reminderRepo: ReminderRepository,
    private val photoStorage: PhotoStorage
) {

    suspend fun purgeExpired(now: Long = System.currentTimeMillis()) {
        val cutoff = now - RETENTION_DAYS * 86_400_000L
        // 先取要删的人物照片（删行后 url 就查不到了），再清行
        personRepo.getDeletedBefore(cutoff).forEach { p ->
            p.photos.forEach { photoStorage.delete(it.url) }
        }
        personRepo.purgeDeletedBefore(cutoff)
        reminderRepo.purgeDeletedBefore(cutoff)
    }

    companion object {
        const val RETENTION_DAYS = 30L

        /** 软删时刻对应的"将彻底清除"时刻，回收站 UI 展示剩余天数用 */
        fun purgeAt(deletedAt: Long): Long = deletedAt + RETENTION_DAYS * 86_400_000L
    }
}
