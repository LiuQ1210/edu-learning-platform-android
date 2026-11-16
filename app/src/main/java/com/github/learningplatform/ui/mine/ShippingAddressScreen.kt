package com.github.learningplatform.ui.mine

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.ui.common.AppCard
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.common.CommonTextField
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.pageGradientBrush

/**
 * 收货信息设置页。
 *
 * 入口在「设置 → 收货信息」。收货地址属于账号级资料，填一次后
 * 兑换奖品时直接带回，不再让用户每次兑换都重复填写。
 */
@Composable
fun ShippingAddressScreen(
    onBack: () -> Unit,
    viewModel: ShippingAddressViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            Toast.makeText(context, "收货信息已保存", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(pageGradientBrush(strong = true)),
        topBar = { BackTopBar(title = "收货信息", onBack = onBack) },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = viewModel::save,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(48.dp)
                ) {
                    Text("保存收货信息", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            AppCard {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(Primary.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                NavIcons.Place,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "收货信息",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(Modifier.weight(1f))
                        Text("用于兑换奖品", fontSize = 11.sp, color = TextHint)
                    }

                    Spacer(Modifier.height(16.dp))

                    CommonTextField(
                        value = uiState.receiver,
                        onValueChange = viewModel::onReceiverChange,
                        label = "收货人",
                        placeholder = "请输入收货人姓名",
                        isError = uiState.receiverError.isNotBlank(),
                        errorMessage = uiState.receiverError
                    )
                    Spacer(Modifier.height(12.dp))

                    CommonTextField(
                        value = uiState.phone,
                        onValueChange = viewModel::onPhoneChange,
                        label = "手机号",
                        placeholder = "请输入 11 位手机号",
                        isError = uiState.phoneError.isNotBlank(),
                        errorMessage = uiState.phoneError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    Spacer(Modifier.height(12.dp))

                    CommonTextField(
                        value = uiState.region,
                        onValueChange = viewModel::onRegionChange,
                        label = "所在地区",
                        placeholder = "如：浙江省 杭州市 西湖区",
                        isError = uiState.regionError.isNotBlank(),
                        errorMessage = uiState.regionError
                    )
                    Spacer(Modifier.height(12.dp))

                    CommonTextField(
                        value = uiState.detail,
                        onValueChange = viewModel::onDetailChange,
                        label = "详细地址",
                        placeholder = "街道、门牌号、学校 / 宿舍楼等",
                        isError = uiState.detailError.isNotBlank(),
                        errorMessage = uiState.detailError,
                        singleLine = false,
                        minLines = 3
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "收货信息仅保存在本机，兑换奖品时自动填入",
                fontSize = 11.sp,
                color = TextHint,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
