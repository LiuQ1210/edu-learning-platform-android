package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.ApiResponse
import com.github.learningplatform.data.remote.dto.CreateNoteRequest
import com.github.learningplatform.data.remote.dto.CreateNoteResponse
import com.github.learningplatform.data.remote.dto.NoteDto
import com.github.learningplatform.data.remote.dto.PageData
import com.github.learningplatform.data.remote.dto.UpdateNoteRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** 笔记模块（接口文档 v3.0 第十章，5 个接口） */
interface NoteApi {

    @GET("notes")
    suspend fun getNotes(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("sourceType") sourceType: Int? = null,
        @Query("keyword") keyword: String? = null
    ): Response<ApiResponse<PageData<NoteDto>>>

    @POST("notes")
    suspend fun createNote(@Body body: CreateNoteRequest): Response<ApiResponse<CreateNoteResponse>>

    @PUT("notes/{noteId}")
    suspend fun updateNote(
        @Path("noteId") noteId: Long,
        @Body body: UpdateNoteRequest
    ): Response<ApiResponse<Unit>>

    @DELETE("notes/{noteId}")
    suspend fun deleteNote(@Path("noteId") noteId: Long): Response<ApiResponse<Unit>>

    /** 某内容下的我的笔记（不分页，按时间倒序） */
    @GET("notes/by-source")
    suspend fun getNotesBySource(
        @Query("sourceType") sourceType: Int,
        @Query("sourceId") sourceId: Long
    ): Response<ApiResponse<List<NoteDto>>>
}