package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.RawTableData
import com.github.learningplatform.data.remote.dto.CourseDto
import com.github.learningplatform.data.remote.dto.TableDataInfo
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/** 搜索模块 ApiV1 接口（热词接口已对接，无数据时由 SearchRepository 回退本地样例） */
interface SearchApi {

    /** 课程搜索 */
    @GET("api/v1/search/course")
    suspend fun searchCourses(
        @Query("keyword") keyword: String?,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<TableDataInfo<CourseDto>>

    /** 视频搜索 */
    @GET("api/v1/search/video")
    suspend fun searchVideos(
        @Query("keyword") keyword: String?,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<RawTableData>

    /** 帖子搜索 */
    @GET("api/v1/search/post")
    suspend fun searchPosts(
        @Query("keyword") keyword: String?,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<RawTableData>

    /**
     * 搜索热词。
     *
     * 后端目前没有搜索日志采集表，`hotWordList` 会返回空数组；
     * 客户端由 SearchRepository 回退本地样例，接口以后有数据自动生效，契约不变。
     *
     * @param size 返回条数，默认 10，后端限制 1~50
     */
    @GET("api/v1/search/hot-words")
    suspend fun getHotWords(@Query("size") size: Int = 10): Response<JsonObject>
}
