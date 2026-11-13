package com.github.learningplatform.data.remote

import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * 积分接口（对齐后端 ApiV1PointsController）。
 *
 *  - `POST /api/v1/points/watch`：帖子阅读秒数上报，满 30 秒 +5；
 *  - `GET /api/v1/points/account`：积分余额（后端尚未实现路由，暂不使用）。
 *
 * 课程视频走 [StudyApi.reportStudy]，由学习上报接口内部结算积分。
 */
interface PointsApi {

    /** 我的积分账户 */
    @GET("api/v1/points/account")
    suspend fun getAccount(): Response<JsonObject>

    /**
     * 帖子阅读时长上报。
     *
     * @param targetType 固定传 `post`
     * @param targetId   帖子 ID
     * @param seconds    本次新增秒数，1~3600（服务端当天累计满 30 秒才发分）
     */
    @FormUrlEncoded
    @POST("api/v1/points/watch")
    suspend fun reportWatch(
        @Field("targetType") targetType: String,
        @Field("targetId") targetId: Long,
        @Field("seconds") seconds: Int
    ): Response<JsonObject>
}
