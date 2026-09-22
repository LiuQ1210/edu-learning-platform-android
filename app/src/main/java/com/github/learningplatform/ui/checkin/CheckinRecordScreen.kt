package com.github.learningplatform.ui.checkin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.ui.common.EmptyState
import com.github.learningplatform.ui.common.ErrorState
import com.github.learningplatform.ui.common.LoadingState
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.pageGradientBrush

/** 签到记录（接口文档 9.3） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckinRecordScreen(
    onBack: () -> Unit,
    viewModel: CheckinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadRecords() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("签到记录") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(NavIcons.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        val m = Modifier.padding(padding).fillMaxSize().background(pageGradientBrush(strong = true))
        when {
            uiState.recordsLoading && uiState.records.isEmpty() -> LoadingState(m)
            uiState.recordsError != null && uiState.records.isEmpty() ->
                ErrorState(message = uiState.recordsError.orEmpty(), modifier = m, onRetry = viewModel::loadRecords)
            uiState.records.isEmpty() -> EmptyState(message = "暂无签到记录", modifier = m, onRetry = viewModel::loadRecords)
            else -> LazyColumn(modifier = m) {
                items(uiState.records, key = { it.recordId }) { r ->
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(r.taskName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (r.success) "成功" else "失败",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (r.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                        Text(
                            r.type.label + " · " + r.signTime,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!r.success && r.failReason.isNotBlank()) {
                            Text(r.failReason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}