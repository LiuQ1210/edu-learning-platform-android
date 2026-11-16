package com.github.learningplatform.data.remote

import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * 学习数据接口（对齐后端 ApiV1StudyController）。
 *
 * 课程播放页把本次新增的播放秒数上报到 `/learn/report`；服务端在记录学习进度、
 * 学习时长的同时，内部调用积分服务结算课程积分。客户端不需要、也不应该再调
 * `/points/watch`，否则同一段秒数会被重复累计。
 */
interface StudyApi {

    /**
     * 上报本次新增学习时长。
     *
     * @param targetType 固定传 `courseVideo`
     * @param videoId    课程小节 ID（不是课程 ID，服务端据此反查课程）
     * @param seconds    本次新增秒数，1~3600
     * @param finished   是否看完，0/1；只影响学习进度，不影响积分
     * @param duration   视频总时长（秒），可选
     */
    @FormUrlEncoded
    @POST("api/v1/learn/report")
    suspend fun reportStudy(
        @Field("targetType") targetType: String,
        @Field("videoId") videoId: Long,
        @Field("seconds") seconds: Int,
        @Field("finished") finished: Int,
        @Field("duration") duration: Int?
    ): Response<JsonObject>
}
