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
 * 用户中心 / 互动列表接口（只声明后端**已实现**的端点）。
 *
 * 后端对应：
 *  - `ApiV1UserProfileController`  GET  /api/v1/user/profile
 *                                  POST /api/v1/user/profile（表单 userName/avatar/sex/bio）
 *  - `ApiV1InteractionController`  GET  /api/v1/interaction/myFavorites?targetType=
 *                                  GET  /api/v1/interaction/myLikes?targetType=
 *  - `ApiV1WatchHistoryController` GET  /api/v1/history/list?targetType=
 *                                  POST /api/v1/history/clear?targetType=
 *
 * 我的课程 / 加入退出课程 / 下载记录 / 他人博客主页 / 签到日历 后端尚未提供，
 * 这些功能在 Repository 里继续用本地样例数据渲染。
 */
interface UserApi {

    /** 个人资料，后端平铺返回 userId/userName/avatar/sex/sexLabel/bio… */
    @GET("api/v1/user/profile")
    suspend fun getProfile(): Response<JsonObject>

    /**
     * 我的主页聚合数据（对齐后端 `GET /api/v1/user/home`）。
     *
     * 平铺返回：`userId / userName / avatar / sex / sexLabel / bio /
     * followCount（关注数）/ fansCount（粉丝数）/ likeReceivedCount（获赞数）/ fanPreview`
     */
    @GET("api/v1/user/home")
    suspend fun getUserHome(): Response<JsonObject>

    /** 保存资料（表单提交，字段同后端 update(userName, avatar, sex, bio)） */
    @FormUrlEncoded
    @POST("api/v1/user/profile")
    suspend fun updateProfile(
        @Field("userName") userName: String? = null,
        @Field("avatar") avatar: String? = null,
        @Field("sex") sex: String? = null,
        @Field("bio") bio: String? = null
    ): Response<JsonObject>

    /** 我的收藏（行结构见后端 MyInteraction） */
    @GET("api/v1/interaction/myFavorites")
    suspend fun getFavorites(
        @Query("targetType") targetType: String? = null,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<RawTableData>

    /** 我的点赞（行结构同收藏） */
    @GET("api/v1/interaction/myLikes")
    suspend fun getLikes(
        @Query("targetType") targetType: String? = null,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<RawTableData>

    /** 观看历史（行结构见后端 WatchHistory） */
    @GET("api/v1/history/list")
    suspend fun getViewHistory(
        @Query("targetType") targetType: String? = null,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<RawTableData>

    /** 清空观看历史 */
    @POST("api/v1/history/clear")
    suspend fun clearViewHistory(
        @Query("targetType") targetType: String? = null
    ): Response<JsonObject>
}
