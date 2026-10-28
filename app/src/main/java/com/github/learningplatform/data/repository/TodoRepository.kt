package com.github.learningplatform.data.repository

import com.github.learningplatform.data.demo.DemoData
import com.github.learningplatform.data.remote.dto.CreateTodoRequest
import com.github.learningplatform.data.remote.dto.PageData
import com.github.learningplatform.data.remote.dto.TodoDto
import com.github.learningplatform.data.remote.dto.UpdateTodoRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 待办仓库。
 *
 * 后端目前**没有**待办相关接口（无 /api/v1/todo*），
 * 按约定先用本地样例数据渲染，不发网络请求，避免 404 把页面变成错误态。
 * 后端补齐待办接口后，把这里替换成 [com.github.learningplatform.data.remote.TodoApi] 调用即可。
 */
@Singleton
class TodoRepository @Inject constructor() {

    suspend fun getTodos(
        pageNum: Int = 1,
        pageSize: Int = 20,
        status: Int? = null,
        priority: Int? = null
    ): PageData<TodoDto> =
        if (status == null) DemoData.todos
        else DemoData.todos.copy(list = DemoData.todos.list.filter { it.status == status })

    /** @return 新建待办 ID（本地占位） */
    suspend fun createTodo(request: CreateTodoRequest): Long = 8999L

    suspend fun updateTodo(todoId: Long, request: UpdateTodoRequest) = Unit

    suspend fun deleteTodo(todoId: Long) = Unit

    /** @param status 0未完成 1已完成 */
    suspend fun updateStatus(todoId: Long, status: Int) = Unit
}
