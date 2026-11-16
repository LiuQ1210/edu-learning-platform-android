package com.github.learningplatform.ui.mall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.local.ExchangeRecord
import com.github.learningplatform.data.local.ExchangeStatus
import com.github.learningplatform.data.local.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExchangeRecordsViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    val records: StateFlow<List<ExchangeRecord>> = userPreferences.exchangeRecords
        .map { list -> list.sortedByDescending { it.createdAt } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            userPreferences.seedExchangeRecordsIfNeeded(demoRecords())
        }
    }

    /** 首次进入时填充的演示记录，保证列表样式可见 */
    private fun demoRecords(): List<ExchangeRecord> {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        return listOf(
            ExchangeRecord(
                id = "demo_youku",
                prizeId = "youku_video_week",
                title = "优酷视频 VIP 周卡",
                emoji = "📺",
                points = 1000,
                createdAt = now - 12 * day,
                status = ExchangeStatus.COMPLETED
            ),
            ExchangeRecord(
                id = "demo_notebook",
                prizeId = "study_notebook",
                title = "精品学习笔记本",
                emoji = "📒",
                points = 500,
                createdAt = now - 4 * day,
                status = ExchangeStatus.SHIPPED
            ),
            ExchangeRecord(
                id = "demo_bookmark",
                prizeId = "bookmark",
                title = "精美书签",
                emoji = "🔖",
                points = 200,
                createdAt = now - 1 * day,
                status = ExchangeStatus.PENDING
            )
        )
    }
}
