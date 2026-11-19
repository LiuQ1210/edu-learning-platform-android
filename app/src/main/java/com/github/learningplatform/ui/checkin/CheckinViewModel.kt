package com.github.learningplatform.ui.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.local.TaskCenterStore
import com.github.learningplatform.data.remote.dto.DailySignStatusDto
import com.github.learningplatform.data.repository.CheckinRepository
import com.github.learningplatform.domain.model.DailyTaskProgress
import com.github.learningplatform.domain.model.TaskCenterRules
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DailySignUiState(
    val isLoading: Boolean = false,
    val status: DailySignStatusDto? = null,
    val message: String? = null,
    /**
     * 加载失败但**不阻塞页面**的错误提示。
     *
     * 总积分统一以 `/checkin/status` 的 `balance` 为准；
     * 签到状态失败时，签到卡显示通用占位，任务列表和商城照常展示。
     */
    val loadError: String? = null,
    val signing: Boolean = false
)

@HiltViewModel
class CheckinViewModel @Inject constructor(
    private val checkinRepository: CheckinRepository,
    private val taskCenterStore: TaskCenterStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailySignUiState())
    val uiState: StateFlow<DailySignUiState> = _uiState.asStateFlow()

    /** 今日任务进度（签到 + 课程视频 + 帖子 + 广告；签到和广告不占内容类 50 分额度） */
    val taskProgress: StateFlow<DailyTaskProgress> = taskCenterStore.progress.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DailyTaskProgress()
    )

    /**
     * 拉取签到状态与余额。
     *
     * 由页面在 ON_RESUME 时调用（首次进入也会收到一次），
     * 这样从课程 / 帖子页回到任务中心时，余额能带上刚获得的积分。
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, loadError = null)
            val previous = _uiState.value

            // 总积分只取 /checkin/status 的 balance；不再请求 /points/account。
            val statusResult = runCatching { checkinRepository.getStatus() }
                .onFailure { if (it is CancellationException) throw it }
            val status = statusResult.getOrNull()

            if (status != null) {
                runCatching {
                    taskCenterStore.syncCheckin(status.signedToday, status.continuousDays)
                }
            }

            val effectiveStatus = status ?: previous.status
            val loadError = if (status == null) {
                val reason = statusResult.exceptionOrNull()?.readableMessage()
                    ?: "网络异常，请稍后重试"
                "签到状态加载失败：$reason"
            } else {
                null
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                status = effectiveStatus,
                loadError = loadError
            )
        }
    }

    fun sign() {
        if (_uiState.value.signing) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(signing = true, message = null)
            try {
                checkinRepository.sign()
                val status = checkinRepository.getStatus()
                val localReward = runCatching {
                    taskCenterStore.syncCheckin(status.signedToday, status.continuousDays)
                }.getOrNull()
                // 客户端按「7 天一个周期、第 1~7 天 +1~+7」的规则展示，服务端余额仍以回包为准
                val message = when {
                    localReward?.alreadyDone == true -> "今日已签到"
                    (localReward?.awarded ?: 0) > 0 ->
                        "签到成功，本周期第 ${TaskCenterRules.signedCycleDay(status.continuousDays)} 天 +${localReward!!.awarded} 积分"
                    else -> "签到成功"
                }
                _uiState.value = _uiState.value.copy(
                    signing = false,
                    message = message,
                    status = status,
                    loadError = null
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(signing = false, message = e.readableMessage())
            }
        }
    }

    /**
     * 广告完整看完后调用。
     *
     * 先上报后端 `/points/ad/reward`，是否入账、今日条数与上限均以服务端返回为准；
     * 上报成功后再把结果同步到本地任务进度缓存（去重 / 上限仅用于本地展示兜底）。
     * 网络失败时不写本地，避免虚假计分。
     */
    fun watchAd(adContentId: String) {
        viewModelScope.launch {
            try {
                val res = checkinRepository.reportAdReward(
                    adId = adContentId,
                    seconds = TaskCenterRules.AD_WATCH_SECONDS
                )
                // 后端已入账或已记录今日广告，按服务端结果同步本地进度缓存用于任务列表展示
                runCatching { taskCenterStore.applyAdResult(adContentId, res.awardedPoints) }
                val message = when {
                    res.awardedPoints > 0 -> "广告观看完成，+${res.awardedPoints} 积分"
                    res.todayAdCount >= res.todayAdLimit ->
                        "今日 ${res.todayAdLimit} 条广告任务已完成"
                    else -> "今日这条广告已计分，明天再来吧"
                }
                _uiState.value = _uiState.value.copy(message = message)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(message = e.readableMessage())
            }
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
