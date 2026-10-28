package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.RawTableData
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 笔记模块 ApiV1 接口（对齐后端 ApiV1NoteController）。
 *
 * 参数名与后端一致：targetType / targetId / noteTitle / noteContent / progress。
 * 返回：add 平铺 noteId；info 为 `data` 包裹；list 为 TableDataInfo。
 */
interface NoteApi {

    /** 新增笔记，返回平铺字段 noteId */
    @POST("api/v1/note/add")
    suspend fun addNote(
        @Query("targetType") targetType: String,
        @Query("targetId") targetId: Long,
        @Query("noteTitle") noteTitle: String,
        @Query("noteContent") noteContent: String,
        @Query("progress") progress: Int? = null
    ): Response<JsonObject>

    /** 修改笔记 */
    @POST("api/v1/note/edit")
    suspend fun editNote(
        @Query("noteId") noteId: Long,
        @Query("noteTitle") noteTitle: String,
        @Query("noteContent") noteContent: String
    ): Response<JsonObject>

    /** 删除笔记 */
    @POST("api/v1/note/remove")
    suspend fun removeNote(
        @Query("noteId") noteId: Long
    ): Response<JsonObject>

    /** 我的笔记列表（分页） */
    @GET("api/v1/note/list")
    suspend fun getNoteList(
        @Query("targetType") targetType: String? = null,
        @Query("targetId") targetId: Long? = null,
        @Query("keyword") keyword: String? = null,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<RawTableData>

    /** 笔记详情（`data` 包裹） */
    @GET("api/v1/note/info")
    suspend fun getNoteInfo(
        @Query("noteId") noteId: Long
    ): Response<JsonObject>
}
