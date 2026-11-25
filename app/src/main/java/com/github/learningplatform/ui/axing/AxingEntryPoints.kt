package com.github.learningplatform.ui.axing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.PrimaryContainer
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary

/** 首页右下角的阿星浮动入口 */
@Composable
fun AxingFloatingEntry(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(3.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = AxingAvatarRes,
                contentDescription = "AI 阿星",
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .offset(y = (-9).dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, Primary.copy(alpha = 0.25f), RoundedCornerShape(50))
                .padding(horizontal = 9.dp, vertical = 2.dp)
        ) {
            Text(
                text = "AI阿星",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Primary
            )
        }
    }
}

/** 课程详情里的「问阿星」入口卡 */
@Composable
fun AxingCourseEntryCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = PrimaryContainer,
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.18f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AxingAvatar(size = 40.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "问阿星",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "讲讲这门课的重点、难点与学习路线",
                    fontSize = 12.sp,
                    color = TextHint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                AxingIcons.ChevronRight,
                contentDescription = null,
                tint = TextHint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
