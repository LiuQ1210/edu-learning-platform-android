package com.github.learningplatform.ui.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.remote.dto.CategoryNodeDto
import com.github.learningplatform.data.repository.CategoryRepository
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryAllUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val tree: List<CategoryNodeDto> = emptyList()
)

@HiltViewModel
class CategoryAllViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryAllUiState())
    val uiState: StateFlow<CategoryAllUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val tree = categoryRepository.getTree(CATEGORY_TYPE_COURSE)
                _uiState.value = CategoryAllUiState(tree = tree)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.readableMessage())
            }
        }
    }
}