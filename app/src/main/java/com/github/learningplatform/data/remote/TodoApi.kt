package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.ApiResponse
import com.github.learningplatform.data.remote.dto.CreateTodoRequest
import com.github.learningplatform.data.remote.dto.CreateTodoResponse
import com.github.learningplatform.data.remote.dto.PageData
import com.github.learningplatform.data.remote.dto.TodoDto
import com.github.learningplatform.data.remote.dto.TodoStatusRequest
import com.github.learningplatform.data.remote.dto.UpdateTodoRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** 待办模块（接口文档 v3.0 第十一章，5 个接口） */
interface TodoApi {

    @GET("todos")
    suspend fun getTodos(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("status") status: Int? = null,
        @Query("priority") priority: Int? = null
    ): Response<ApiResponse<PageData<TodoDto>>>

    @POST("todos")
    suspend fun createTodo(@Body body: CreateTodoRequest): Response<ApiResponse<CreateTodoResponse>>

    @PUT("todos/{todoId}")
    suspend fun updateTodo(
        @Path("todoId") todoId: Long,
        @Body body: UpdateTodoRequest
    ): Response<ApiResponse<Unit>>

    @DELETE("todos/{todoId}")
    suspend fun deleteTodo(@Path("todoId") todoId: Long): Response<ApiResponse<Unit>>

    /** 标记完成 / 取消完成 */
    @PATCH("todos/{todoId}/status")
    suspend fun updateStatus(
        @Path("todoId") todoId: Long,
        @Body body: TodoStatusRequest
    ): Response<ApiResponse<Unit>>
}