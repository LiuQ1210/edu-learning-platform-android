package com.github.learningplatform.data.remote.dto

/**
 * 内容观看积分结算结果（课程视频 / 帖子共用）。
 *
 * 服务端 `/learn/report` 与 `/points/watch` 返回的积分字段一致，客户端只需一套模型。
 */
data class ContentWatchResult(
    /** 本次实际入账积分：0 或 5 */
    val awardedPoints: Int = 0,
    /** 当天课程 + 帖子合计已得积分（服务端权威值） */
    val todayContentPoints: Int = 0,
    /** 当天内容积分上限，固定 50 */
    val todayContentLimit: Int = 0,
    /** 该内容当天累计观看秒数 */
    val watchedSeconds: Int = 0
)
