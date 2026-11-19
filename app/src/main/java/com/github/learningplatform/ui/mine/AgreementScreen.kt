package com.github.learningplatform.ui.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.common.ErrorState
import com.github.learningplatform.ui.common.LoadingState
import com.github.learningplatform.ui.common.RichHtmlContent
import com.github.learningplatform.ui.theme.pageGradientBrush

/** 协议类型（对齐后端 Agreement 常量） */
object AgreementType {
    const val USER_AGREEMENT = "user_agreement"
    const val PRIVACY_POLICY = "privacy_policy"
    const val ABOUT_US = "about_us"
}

/** 后端标题为空时的本地兜底中文名 */
fun agreementFallbackTitle(type: String): String = when (type) {
    AgreementType.USER_AGREEMENT -> "用户协议"
    AgreementType.PRIVACY_POLICY -> "隐私政策"
    AgreementType.ABOUT_US -> "关于我们"
    else -> "协议"
}

/**
 * 协议详情全屏页：拉取后端 HTML 正文并用 WebView 渲染。
 * 入口：设置页「用户协议 / 隐私政策 / 关于我们」。
 */
@Composable
fun AgreementScreen(
    agreementType: String,
    onBack: () -> Unit,
    viewModel: AgreementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(agreementType) { viewModel.load(agreementType) }

    val fallbackTitle = remember(agreementType) { agreementFallbackTitle(agreementType) }

    Scaffold(
        topBar = {
            BackTopBar(
                title = uiState.title.ifBlank { fallbackTitle },
                onBack = onBack
            )
        }
    ) { padding ->
        when {
            uiState.loading -> LoadingState(Modifier.padding(padding))

            uiState.error != null -> ErrorState(
                message = uiState.error.orEmpty(),
                modifier = Modifier.padding(padding),
                onRetry = { viewModel.load(agreementType) }
            )

            else -> Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(pageGradientBrush(strong = true))
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                RichHtmlContent(
                    html = uiState.contentHtml,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
