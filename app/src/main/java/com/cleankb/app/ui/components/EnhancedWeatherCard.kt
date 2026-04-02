package com.cleankb.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleankb.app.data.CampusService
import com.cleankb.app.ui.theme.Radius
import com.cleankb.app.ui.theme.Spacing

/**
 * 增强版天气卡片 - 完整天气信息展示
 *
 * 设计特点：
 * - 渐变背景随天气状态动态变化
 * - 大号温度数字配合天气图标
 * - 体感温度和温度等级提示
 * - 动画过渡效果
 */
@Composable
fun EnhancedWeatherCard(
    data: CampusService.WeatherSummary?,
    loading: Boolean,
    error: String?,
    isDark: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientColors = when {
        data?.rainToday == true -> listOf(
            if (isDark) Color(0xFF1A2744) else Color(0xFFEFF6FF),
            if (isDark) Color(0xFF1E3A5F) else Color(0xFFDBEAFE)
        )
        data?.maxTempC != null && data.maxTempC >= 30 -> listOf(
            if (isDark) Color(0xFF3D2E1A) else Color(0xFFFFF7ED),
            if (isDark) Color(0xFF4A3520) else Color(0xFFFFEDD5)
        )
        else -> listOf(
            if (isDark) Color(0xFF2D3556) else Color(0xFFEEF2FF),
            if (isDark) Color(0xFF3D4566) else Color(0xFFE0E7FF)
        )
    }

    val animatedGradientStart by animateColorAsState(
        targetValue = gradientColors.first(),
        animationSpec = tween(durationMillis = 800),
        label = "gradientStart"
    )
    val animatedGradientEnd by animateColorAsState(
        targetValue = gradientColors.last(),
        animationSpec = tween(durationMillis = 800),
        label = "gradientEnd"
    )

    val shape = RoundedCornerShape(Radius.lg)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = shape,
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
            .clip(shape)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(animatedGradientStart, animatedGradientEnd)
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactWeatherIcon(
                weatherText = data?.currentWeatherText ?: "",
                rainToday = data?.rainToday ?: false,
                isDark = isDark
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Text(
                        text = "${fmtTemp(data?.currentTempC ?: 0.0)}°",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = data?.currentWeatherText ?: "加载中...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    if (data != null) {
                        Text(
                            text = "${fmtTemp(data.minTempC)}°~${fmtTemp(data.maxTempC)}°",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Air,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format("%.0f", data.windSpeedMps)}m/s",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "设置",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactWeatherIcon(
    weatherText: String,
    rainToday: Boolean,
    isDark: Boolean
) {
    val icon: ImageVector
    val iconColor: Color

    when {
        rainToday -> {
            icon = Icons.Filled.Umbrella
            iconColor = if (isDark) Color(0xFF64B5F6) else Color(0xFF1976D2)
        }
        weatherText.contains("晴") -> {
            icon = Icons.Filled.WbSunny
            iconColor = if (isDark) Color(0xFFFFB74D) else Color(0xFFFFA000)
        }
        weatherText.contains("云") || weatherText.contains("阴") -> {
            icon = Icons.Outlined.WbCloudy
            iconColor = if (isDark) Color(0xFF90A4AE) else Color(0xFF607D8B)
        }
        else -> {
            icon = Icons.Filled.WbSunny
            iconColor = if (isDark) Color(0xFFFFB74D) else Color(0xFFFFA000)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "weatherIcon")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconPulse"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = weatherText,
            tint = iconColor,
            modifier = Modifier
                .size(28.dp)
                .alpha(pulse)
        )
    }
}

@Composable
private fun WeatherLoadingState() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loadingAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(Radius.full))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 16.dp)
                    .clip(RoundedCornerShape(Radius.xs))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
            )
            Box(
                modifier = Modifier
                    .size(width = 72.dp, height = 12.dp)
                    .clip(RoundedCornerShape(Radius.xs))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha * 0.7f))
            )
        }
    }
}

@Composable
private fun WeatherEmptyState() {
    Row(
        modifier = Modifier.padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Icon(
            imageVector = Icons.Outlined.WbCloudy,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = "天气暂无数据",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun fmtTemp(v: Double): String = kotlin.math.round(v).toInt().toString()