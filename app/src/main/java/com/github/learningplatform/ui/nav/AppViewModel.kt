package com.github.learningplatform.ui.nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.local.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AppStartState { LOADING, LOGGED_IN, LOGGED_OUT }

/**
 * 应用启动态。
 *
 * 原来的实现把 startDestination 硬编码为 login，导致每次冷启动都要重新登录；
 * 这里改为读取 DataStore 中的 accessToken 后三态分发。
 *
 * 注意不能直接 collect UserPreferences.isLoggedIn —— DataStore.data 是永不完成的 Flow，
 * 用它当 startDestination 的来源会让 NavHost 的图结构在运行中重建。
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _startState = MutableStateFlow(AppStartState.LOADING)
    val startState: StateFlow<AppStartState> = _startState.asStateFlow()

    init {
        viewModelScope.launch {
            val loggedIn = runCatching { userPreferences.accessToken.first().isNotBlank() }
                .getOrDefault(false)
            _startState.value = if (loggedIn) AppStartState.LOGGED_IN else AppStartState.LOGGED_OUT
        }
    }
}