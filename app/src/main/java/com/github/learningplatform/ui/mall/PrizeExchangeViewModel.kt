package com.github.learningplatform.ui.mall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.remote.dto.ExchangeResultDto
import com.github.learningplatform.data.remote.dto.ShopAddressDto
import com.github.learningplatform.data.repository.ShopRepository
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExchangeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val product: MallPrize? = null,
    val address: ShopAddressDto? = null
)

/** 兑换确认页：商品详情 + 默认收货地址来自后端，确认兑换调用 `/shop/exchange`。 */
@HiltViewModel
class PrizeExchangeViewModel @Inject constructor(
    private val shopRepository: ShopRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExchangeUiState())
    val uiState: StateFlow<ExchangeUiState> = _uiState.asStateFlow()

    fun load(productId: String) {
        viewModelScope.launch {
            _uiState.value = ExchangeUiState(isLoading = true)
            try {
                val product = shopRepository.getProductDetail(productId.toLong()).toMallPrize()
                val addresses = runCatching { shopRepository.getAddresses() }.getOrDefault(emptyList())
                val address = addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull()
                _uiState.value = ExchangeUiState(isLoading = false, product = product, address = address)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = ExchangeUiState(isLoading = false, error = e.readableMessage())
            }
        }
    }

    /** 提交兑换，返回兑换码、消耗积分与兑换后余额 */
    suspend fun exchange(productId: String): ExchangeResultDto =
        shopRepository.exchange(productId.toLong())
}
