package com.github.learningplatform.ui.mall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.local.ExchangeRecord
import com.github.learningplatform.data.local.ExchangeStatus
import com.github.learningplatform.data.local.ShippingAddress
import com.github.learningplatform.data.local.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.util.UUID
import javax.inject.Inject

/**
 * 兑换确认页只读取收货信息；修改入口跳到「设置 → 收货信息」。
 * 用 StateFlow 订阅 DataStore，从设置页返回时地址卡片会自动刷新。
 * 确认兑换时把记录写入本机，供兑换记录页展示。
 */
@HiltViewModel
class PrizeExchangeViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    val shippingAddress: StateFlow<ShippingAddress> = userPreferences.shippingAddress.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ShippingAddress()
    )

    suspend fun recordExchange(prize: MallPrize) {
        userPreferences.addExchangeRecord(
            ExchangeRecord(
                id = UUID.randomUUID().toString(),
                prizeId = prize.id,
                title = prize.title,
                emoji = prize.emoji,
                points = prize.points,
                createdAt = System.currentTimeMillis(),
                status = ExchangeStatus.forCategory(prize.categoryKey)
            )
        )
    }
}
