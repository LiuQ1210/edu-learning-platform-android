package com.github.learningplatform.data.local

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.github.learningplatform.data.remote.dto.ContentWatchResult
import com.github.learningplatform.domain.model.ContentTaskType
import com.github.learningplatform.domain.model.DailyTaskProgress
import com.github.learningplatform.domain.model.TaskCenterRules
import com.github.learningplatform.domain.model.TaskRewardResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 任务中心本地展示缓存。
 *
 * 积分是否发放、每天 50 分上限是否已满，全部由服务端判定；这里只缓存
 * 「服务端返回的今日内容积分」和「今日已计分的内容数量」，供任务中心展示。
 * 本地不做任何积分加减，也不保存真实余额。
 *
 * 课程视频、帖子、广告的「今日是否已计分」按内容 id 记录：
 *  - 课程视频 / 帖子：同一内容每天只计一次，次日可重新计分；
 *  - 广告：同一广告每天只计一次，每天最多 [TaskCenterRules.AD_DAILY_LIMIT] 条，
 *    广告积分不占用内容类 50 分额度；
 *  - 签到积分单独计算，不占用内容类额度。
 *
 * 存储复用 [appDataStore]，退出登录 / 改密时 [UserPreferences.clear] 会一并清掉。
 */
@Singleton
class TaskCenterStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private object Keys {
        val DATE = stringPreferencesKey("task_date")

        val CHECKIN_POINTS = intPreferencesKey("checkin_points")
        val CHECKIN_RECORDED = booleanPreferencesKey("checkin_recorded")
        val AD_POINTS = intPreferencesKey("ad_points")

        /** 服务端返回的今日课程 + 帖子积分 */
        val SERVER_CONTENT_POINTS = intPreferencesKey("server_content_points")

        /** 今日已计分的课程视频 / 文章 / 广告 id（跨天清空） */
        val VIDEO_IDS_TODAY = stringSetPreferencesKey("video_ids_today")
        val ARTICLE_IDS_TODAY = stringSetPreferencesKey("article_ids_today")
        val AD_IDS_TODAY = stringSetPreferencesKey("ad_ids_today")
    }

    /** 所有写操作串行化，避免跨天重置与上报回写互相覆盖 */
    private val mutex = Mutex()

    /** 今日任务进度；跨天读取时自动按空进度展示 */
    val progress: Flow<DailyTaskProgress> = context.appDataStore.data.map { prefs ->
        prefs.toProgress(today())
    }

    /**
     * 用签到状态对齐本地签到积分。
     *
     * 每次进入任务中心都会调用：如果服务端显示「今天已签」而本地还没记过
     * （换设备、重装、首次接入），按连续天数补记；已记过则幂等返回。
     */
    suspend fun syncCheckin(signedToday: Boolean, continuousDays: Int): TaskRewardResult =
        mutex.withLock {
            if (!signedToday) return@withLock TaskRewardResult()

            val today = today()
            val prefs = context.appDataStore.data.first()
            if (prefs[Keys.DATE] == today && prefs[Keys.CHECKIN_RECORDED] == true) {
                return@withLock TaskRewardResult(alreadyDone = true)
            }

            // 签到积分单独计算，不占用内容类每日 50 分额度
            val awarded = TaskCenterRules.checkinPointsForSignedDay(continuousDays)
            context.appDataStore.edit { p ->
                if (p[Keys.DATE] != today) resetDaily(p, today)
                p[Keys.CHECKIN_RECORDED] = true
                p[Keys.CHECKIN_POINTS] = awarded
            }
            TaskRewardResult(awarded = awarded)
        }

    /**
     * 用服务端结算结果更新展示缓存。
     *
     * - [ContentWatchResult.todayContentPoints] 覆盖当天内容积分（取较大值，防止
     *   并发响应乱序把数字改小）；
     * - 只有当天累计观看满 30 秒才计入「今日已看数量」，与发分口径一致；
     * - 同一内容当天重复上报不会重复计数。
     */
    suspend fun applyContentResult(
        type: ContentTaskType,
        contentId: Long,
        result: ContentWatchResult
    ) = mutex.withLock {
        if (contentId <= 0L) return@withLock
        val today = today()
        context.appDataStore.edit { p ->
            if (p[Keys.DATE] != today) resetDaily(p, today)

            val serverPoints = result.todayContentPoints
                .coerceIn(0, TaskCenterRules.CONTENT_DAILY_CAP)
            p[Keys.SERVER_CONTENT_POINTS] =
                maxOf(p[Keys.SERVER_CONTENT_POINTS] ?: 0, serverPoints)

            if (result.watchedSeconds >= TaskCenterRules.MIN_WATCH_SECONDS) {
                val idsKey = when (type) {
                    ContentTaskType.VIDEO -> Keys.VIDEO_IDS_TODAY
                    ContentTaskType.ARTICLE -> Keys.ARTICLE_IDS_TODAY
                }
                p[idsKey] = p[idsKey].orEmpty() + contentId.toString()
            }
        }
    }

    /** 记录完整看完的一条广告；同一广告每天只计一次，每天最多 3 条。 */
    suspend fun recordAd(contentId: String): TaskRewardResult = mutex.withLock {
        if (contentId.isBlank()) return@withLock TaskRewardResult()
        val today = today()
        val prefs = context.appDataStore.data.first()

        val todayAdIds = if (prefs[Keys.DATE] == today) {
            prefs[Keys.AD_IDS_TODAY].orEmpty()
        } else {
            emptySet()
        }
        if (contentId in todayAdIds) return@withLock TaskRewardResult(alreadyDone = true)
        if (todayAdIds.size >= TaskCenterRules.AD_DAILY_LIMIT) {
            return@withLock TaskRewardResult(capped = true)
        }

        context.appDataStore.edit { p ->
            if (p[Keys.DATE] != today) resetDaily(p, today)
            p[Keys.AD_IDS_TODAY] = p[Keys.AD_IDS_TODAY].orEmpty() + contentId
            p[Keys.AD_POINTS] = (p[Keys.AD_POINTS] ?: 0) + TaskCenterRules.AD_REWARD
        }
        TaskRewardResult(awarded = TaskCenterRules.AD_REWARD)
    }

    // ------------------------------------------------------------------ 内部

    private fun Preferences.toProgress(today: String): DailyTaskProgress {
        // 日期不一致（含首次使用 / 跨天）时按空进度展示，真正的清零在首次写入时落盘
        if (this[Keys.DATE] != today) return DailyTaskProgress(date = today)
        return DailyTaskProgress(
            date = today,
            checkinPoints = this[Keys.CHECKIN_POINTS] ?: 0,
            checkinRecorded = this[Keys.CHECKIN_RECORDED] ?: false,
            rawContentPoints = this[Keys.SERVER_CONTENT_POINTS] ?: 0,
            videoCount = this[Keys.VIDEO_IDS_TODAY].orEmpty().size,
            articleCount = this[Keys.ARTICLE_IDS_TODAY].orEmpty().size,
            adPoints = this[Keys.AD_POINTS] ?: 0,
            adCount = this[Keys.AD_IDS_TODAY].orEmpty().size
        )
    }

    /** 跨天重置：清空当天的积分与 id */
    private fun resetDaily(p: MutablePreferences, today: String) {
        p[Keys.DATE] = today
        p[Keys.CHECKIN_POINTS] = 0
        p[Keys.CHECKIN_RECORDED] = false
        p[Keys.SERVER_CONTENT_POINTS] = 0
        p[Keys.AD_POINTS] = 0
        p[Keys.VIDEO_IDS_TODAY] = emptySet()
        p[Keys.ARTICLE_IDS_TODAY] = emptySet()
        p[Keys.AD_IDS_TODAY] = emptySet()
    }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
}
