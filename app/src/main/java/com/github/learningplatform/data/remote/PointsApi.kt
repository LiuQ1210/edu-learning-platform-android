package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.RawTableData
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 积分接口（对齐后端 ApiV1PointsController）。
 *
 *  - `POST /api/v1/points/watch`：帖子阅读秒数上报，满 30 秒 +5；
 *  - `GET /api/v1/points/account`：积分余额；
 *  - `GET /api/v1/points/logs`：积分明细分页（商城「积分明细」）；
 *  - `POST /api/v1/points/ad/reward`：看完广告领积分。
 *
 * 课程视频走 [StudyApi.reportStudy]，由学习上报接口内部结算积分。
 * 免广告兑换（`/points/exchange`、`/points/exchange/tiers`）产品已下线，前端不再调用。
 */
interface PointsApi {

    /** 我的积分账户 */
    @GET("api/v1/points/account")
    suspend fun getAccount(): Response<JsonObject>

    /**
     * 积分明细（分页），后端按 `create_time desc, log_id desc` 排序。
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页条数，后端限制 1~50
     * @param type     可选来源编码：checkin / watchCourse / watchPost / watchAd /
     *                 shopExchange / exchange / adjust；不传或空串表示全部
     */
    @GET("api/v1/points/logs")
    suspend fun getLogs(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("type") type: String? = null
    ): Response<RawTableData>

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

    /**
     * 看完广告领积分。
     *
     * @param adId   广告唯一标识（最长 64）
     * @param seconds 本次观看秒数（后端要求至少 15 秒）
     * 返回 awardedPoints / todayAdCount / todayAdLimit；重复广告或达每日上限时 awardedPoints 为 0。
     */
    @FormUrlEncoded
    @POST("api/v1/points/ad/reward")
    suspend fun reportAdReward(
        @Field("adId") adId: String,
        @Field("seconds") seconds: Int
    ): Response<JsonObject>
}
