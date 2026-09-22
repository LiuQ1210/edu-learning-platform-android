package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.safeApiCall
import com.github.learningplatform.core.network.safeApiCallForUnit
import com.github.learningplatform.data.demo.DemoData
import com.github.learningplatform.data.demo.preview
import com.github.learningplatform.data.demo.previewUnit
import com.github.learningplatform.data.remote.TodoApi
import com.github.learningplatform.data.remote.dto.CreateTodoRequest
import com.github.learningplatform.data.remote.dto.PageData
import com.github.learningplatform.data.remote.dto.TodoDto
import com.github.learningplatform.data.remote.dto.TodoStatusRequest
import com.github.learningplatform.data.remote.dto.UpdateTodoRequest
import javax.inject.Inject
import javax.inject.Singleton

/** 待办仓库（接口文档 v3.0 第十一章） */
@Singleton
class TodoRepository @Inject constructor(
    private val todoApi: TodoApi
) {

    suspend fun getTodos(
        pageNum: Int = 1,
        pageSize: Int = 20,
        status: Int? = null,
        priority: Int? = null
    ): PageData<TodoDto> {
        preview { return DemoData.todos }
        return safeApiCall {
            todoApi.getTodos(pageNum, pageSize, status, priority)
        }
    }

    /** @return 新建待办 ID */
    suspend fun createTodo(request: CreateTodoRequest): Long {
        preview { return 8999L }
        return safeApiCall { todoApi.createTodo(request) }.todoId
    }

    suspend fun updateTodo(todoId: Long, request: UpdateTodoRequest) {
        previewUnit { return }
        safeApiCallForUnit { todoApi.updateTodo(todoId, request) }
    }

    suspend fun deleteTodo(todoId: Long) {
        previewUnit { return }
        safeApiCallForUnit { todoApi.deleteTodo(todoId) }
    }

    /** @param status 0未完成 1已完成 */
    suspend fun updateStatus(todoId: Long, status: Int) {
        previewUnit { return }
        safeApiCallForUnit { todoApi.updateStatus(todoId, TodoStatusRequest(status)) }
    }
}
