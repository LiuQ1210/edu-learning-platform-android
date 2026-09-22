package com.github.learningplatform.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.remote.dto.CreateTodoRequest
import com.github.learningplatform.data.remote.dto.TodoDto
import com.github.learningplatform.data.repository.TodoRepository
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TodosUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val status: Int? = null,
    val todos: List<TodoDto> = emptyList(),
    val showCreateDialog: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class TodosViewModel @Inject constructor(
    private val todoRepository: TodoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodosUiState())
    val uiState: StateFlow<TodosUiState> = _uiState.asStateFlow()

    init { load() }

    fun refresh() = load()

    fun selectStatus(status: Int?) {
        if (status == _uiState.value.status) return
        _uiState.value = _uiState.value.copy(status = status)
        load()
    }

    fun openCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true)
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun dismissCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }

    fun create(title: String, description: String, priority: Int, deadline: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showCreateDialog = false)
            try {
                todoRepository.createTodo(
                    CreateTodoRequest(
                        title = title,
                        description = description.ifBlank { null },
                        priority = priority,
                        deadline = deadline
                    )
                )
                load()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(message = e.readableMessage())
            }
        }
    }

    /** 标记完成 / 取消完成（接口 11.5 PATCH） */
    fun toggleStatus(todo: TodoDto) {
        val next = if (todo.status == 1) 0 else 1
        viewModelScope.launch {
            try {
                todoRepository.updateStatus(todo.todoId, next)
                // 当前筛选与目标状态不一致时，直接移除该行，避免出现「已完成」混在未完成里
                val filter = _uiState.value.status
                val updated = _uiState.value.todos.map {
                    if (it.todoId == todo.todoId) it.copy(status = next) else it
                }
                _uiState.value = _uiState.value.copy(
                    todos = if (filter != null && filter != next) updated.filterNot { it.todoId == todo.todoId }
                    else updated
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(message = e.readableMessage())
            }
        }
    }

    fun delete(todoId: Long) {
        viewModelScope.launch {
            try {
                todoRepository.deleteTodo(todoId)
                _uiState.value = _uiState.value.copy(
                    todos = _uiState.value.todos.filterNot { it.todoId == todoId }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(message = e.readableMessage())
            }
        }
    }

    private fun load() {
        val snapshot = _uiState.value
        viewModelScope.launch {
            _uiState.value = snapshot.copy(isLoading = true, error = null)
            try {
                val page = todoRepository.getTodos(
                    pageNum = 1,
                    pageSize = 100,
                    status = snapshot.status
                )
                _uiState.value = _uiState.value.copy(isLoading = false, todos = page.list)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.readableMessage())
            }
        }
    }
}