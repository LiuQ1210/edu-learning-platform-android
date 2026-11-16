package com.github.learningplatform.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.github.learningplatform.data.ad.TakuAds
import com.github.learningplatform.data.local.PrivacyConsent

/**
 * 首次启动的隐私告知闸门。
 *
 * 广告 SDK 必须在用户同意隐私政策之后才初始化，否则广告请求会被 SDK 拦截，
 * 同时构成违规收集个人信息。
 */
@Composable
fun PrivacyConsentGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var agreed by remember { mutableStateOf(PrivacyConsent.isAgreed(context)) }
    var dialogVisible by remember { mutableStateOf(!agreed) }

    LaunchedEffect(agreed) {
        if (agreed) TakuAds.init(context)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        if (dialogVisible) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("隐私政策提示") },
                text = {
                    Text(
                        "为保障你的正常使用，我们会收集设备信息（设备标识、网络状态等）用于广告展示与统计分析，" +
                            "详见《用户协议》和《隐私政策》。"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            PrivacyConsent.agree(context)
                            agreed = true
                            dialogVisible = false
                        }
                    ) {
                        Text("同意并接受")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dialogVisible = false }) {
                        Text("不同意")
                    }
                },
            )
        }
    }
}
