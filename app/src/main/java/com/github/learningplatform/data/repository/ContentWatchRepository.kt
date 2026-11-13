package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.int
import com.github.learningplatform.core.network.okBody
import com.github.learningplatform.core.network.payload
import com.github.learningplatform.data.remote.PointsApi
import com.github.learningplatform.data.remote.StudyApi
import com.github.learningplatform.data.remote.dto.ContentWatchResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonObject

/** 内容观看上报（课程视频 / 帖子），积分由服务端判定。 */
@Singleton
class ContentWatchRepository @Inject constructor(
    private val studyApi: StudyApi,
    private val pointsApi: PointsApi
) {

    /**
     * 上报课程视频本次新增播放秒数，由 `/learn/report` 内部结算积分。
     *
     * @param lessonId 课程小节 ID，不能传课程 ID
     * @param seconds 本次新增秒数，调用方需保证 1~3600
     */
    suspend fun reportCourseVideo(
        lessonId: Long,
        seconds: Int,
        finished: Boolean,
        durationSeconds: Int?
    ): ContentWatchResult {
        val body = studyApi.reportStudy(
            targetType = TARGET_COURSE_VIDEO,
            videoId = lessonId,
            seconds = seconds,
            finished = if (finished) 1 else 0,
            duration = durationSeconds?.takeIf { it > 0 }
        ).okBody().payload()
        return body.toResult()
    }

    /** 上报帖子本次新增阅读秒数，满 30 秒由服务端发分。 */
    suspend fun reportPost(postId: Long, seconds: Int): ContentWatchResult {
        val body = pointsApi.reportWatch(
            targetType = TARGET_POST,
            targetId = postId,
            seconds = seconds
        ).okBody().payload()
        return body.toResult()
    }

    /**
     * 两个接口的返回字段并不统一：帖子返回 `watchedSeconds`，
     * 课程 `/learn/report` 返回的当日累计时长叫 `todaySeconds`。
     * 只认前者会让课程永远停在「今日已看 0 个」。
     */
    private fun JsonObject.toResult() = ContentWatchResult(
        awardedPoints = int("awardedPoints"),
        todayContentPoints = int("todayContentPoints"),
        todayContentLimit = int("todayContentLimit"),
        watchedSeconds = int("watchedSeconds").takeIf { it > 0 } ?: int("todaySeconds")
    )

    private companion object {
        const val TARGET_COURSE_VIDEO = "courseVideo"
        const val TARGET_POST = "post"
    }
}
