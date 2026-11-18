package com.github.learningplatform.ui.mall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.remote.dto.ShopOrderDto
import com.github.learningplatform.data.repository.ShopRepository
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecordsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val orders: List<ShopOrderDto> = emptyList()
)

/** 我的兑换记录（后端 GET /api/v1/shop/order/list，只能看自己） */
@HiltViewModel
class ExchangeRecordsViewModel @Inject constructor(
    private val shopRepository: ShopRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordsUiState())
    val uiState: StateFlow<RecordsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = RecordsUiState(isLoading = true)
            try {
                val orders = shopRepository.getOrders().list
                _uiState.value = RecordsUiState(isLoading = false, orders = orders)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = RecordsUiState(isLoading = false, error = e.readableMessage())
            }
        }
    }
}
