package com.github.learningplatform.ui.mall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.remote.dto.ShopCategoryDto
import com.github.learningplatform.data.repository.ShopRepository
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MallUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val products: List<MallPrize> = emptyList(),
    val categories: List<ShopCategoryDto> = emptyList()
)

/** 积分商城首页：商品列表 + 分类字典，数据来自后端 shopping 模块。 */
@HiltViewModel
class MallViewModel @Inject constructor(
    private val shopRepository: ShopRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MallUiState())
    val uiState: StateFlow<MallUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val products = shopRepository.getProducts().list.map { it.toMallPrize() }
                val categories = runCatching { shopRepository.getCategories() }.getOrDefault(emptyList())
                _uiState.value = MallUiState(isLoading = false, products = products, categories = categories)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.readableMessage()
                )
            }
        }
    }
}
