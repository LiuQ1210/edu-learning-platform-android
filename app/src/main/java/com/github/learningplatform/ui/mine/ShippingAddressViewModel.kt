package com.github.learningplatform.ui.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.local.ShippingAddress
import com.github.learningplatform.data.local.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShippingAddressUiState(
    val receiver: String = "",
    val phone: String = "",
    val region: String = "",
    val detail: String = "",
    /** 点过保存后才展示校验错误，避免一进页面就飘红 */
    val attempted: Boolean = false,
    val saved: Boolean = false
) {
    val phoneValid: Boolean
        get() = phone.matches(Regex("^1[3-9]\\d{9}$"))

    val receiverError: String
        get() = if (attempted && receiver.isBlank()) "请填写收货人姓名" else ""

    val phoneError: String
        get() = when {
            !attempted -> ""
            phone.isBlank() -> "请填写手机号"
            !phoneValid -> "请输入 11 位手机号"
            else -> ""
        }

    val regionError: String
        get() = if (attempted && region.isBlank()) "请填写所在地区" else ""

    val detailError: String
        get() = if (attempted && detail.isBlank()) "请填写详细地址" else ""

    val isValid: Boolean
        get() = receiver.isNotBlank() &&
            phoneValid &&
            region.isNotBlank() &&
            detail.isNotBlank()
}

@HiltViewModel
class ShippingAddressViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShippingAddressUiState())
    val uiState: StateFlow<ShippingAddressUiState> = _uiState.asStateFlow()

    init {
        // 回填已保存的收货信息，便于用户只改其中一项
        viewModelScope.launch {
            val saved = userPreferences.shippingAddress.first()
            _uiState.value = _uiState.value.copy(
                receiver = saved.receiver,
                phone = saved.phone,
                region = saved.region,
                detail = saved.detail
            )
        }
    }

    fun onReceiverChange(value: String) {
        _uiState.value = _uiState.value.copy(receiver = value)
    }

    fun onPhoneChange(value: String) {
        _uiState.value = _uiState.value.copy(phone = value.filter { it.isDigit() }.take(11))
    }

    fun onRegionChange(value: String) {
        _uiState.value = _uiState.value.copy(region = value)
    }

    fun onDetailChange(value: String) {
        _uiState.value = _uiState.value.copy(detail = value)
    }

    fun save() {
        val state = _uiState.value.copy(attempted = true)
        _uiState.value = state
        if (!state.isValid) return

        viewModelScope.launch {
            userPreferences.saveShippingAddress(
                ShippingAddress(
                    receiver = state.receiver,
                    phone = state.phone,
                    region = state.region,
                    detail = state.detail
                )
            )
            _uiState.value = _uiState.value.copy(saved = true)
        }
    }
}
