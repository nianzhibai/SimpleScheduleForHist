package com.cleankb.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleankb.app.data.CampusService
import com.cleankb.app.ui.theme.Radius
import com.cleankb.app.ui.theme.Spacing

/**
 * 天气详情页 - 完整的天气信息展示
 *
 * 页面结构：
 * 1. 天气主卡片（渐变背景 + 温度 + 天气图标）
 * 2. 下雨提醒（如果有雨）
 * 3. 详细信息行（风速/日出/日落）
 * 4. 穿衣建议
 * 5. 温馨提示列表
 */
@Composable
fun WeatherDetailPage(
    data: CampusService.WeatherSummary?,
    loading: Boolean,
    error: String?,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    if (loading && data == null) {
        WeatherPageSkeleton()
        return
    }

    if (data == null && error != null) {
        WeatherPageError(error = error)
        return
    }

    if (data == null) {
        WeatherPageEmpty()
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // 天气主卡片
        item {
            WeatherHeroCard(
                data = data,
                isDark = isDark
            )
        }

        // 下雨提醒
        if (data.rainToday) {
            item {
                WeatherRainAlert(data = data)
            }
        }

        // 详细信息
        item {
            WeatherInfoCard(data = data)
        }

        // 穿衣建议
        if (data.coreAdvice.isNotBlank() || data.temperatureLevel.isNotBlank()) {
            item {
                WeatherDressingAdvice(data = data)
            }
        }

        // 温馨提示
        if (data.extraTips.isNotEmpty()) {
            item {
                WeatherTipsSection(tips = data.extraTips)
            }
        }
    }
}

/**
 * 天气英雄卡片 - 紧凑版
 */
@Composable
private fun WeatherHeroCard(
    data: CampusService.WeatherSummary,
    isDark: Boolean
) {
    // 根据天气状态选择渐变色
    val gradientColors = when {
        data.rainToday -> listOf(
            if (isDark) Color(0xFF1A2744) else Color(0xFFEFF6FF),
            if (isDark) Color(0xFF1E3A5F) else Color(0xFFDBEAFE)
        )
        data.maxTempC >= 30 -> listOf(
            if (isDark) Color(0xFF3D2E1A) else Color(0xFFFFF7ED),
            if (isDark) Color(0xFF4A3520) else Color(0xFFFFEDD5)
        )
        data.minTempC <= 10 -> listOf(
            if (isDark) Color(0xFF1A2E3D) else Color(0xFFF0F9FF),
            if (isDark) Color(0xFF1E3A5F) else Color(0xFFE0F2FE)
        )
        else -> listOf(
            if (isDark) Color(0xFF2D3556) else Color(0xFFEEF2FF),
            if (isDark) Color(0xFF3D4566) else Color(0xFFE0E7FF)
        )
    }

    val shape = RoundedCornerShape(Radius.xl)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = shape,
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.verticalGradient(colors = gradientColors))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                // 位置信息
                Text(
                    text = data.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 天气核心信息：图标+天气文字 | 温度
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 天气图标 + 天气描述
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        WeatherAnimatedIconLarge(
                            weatherText = data.currentWeatherText,
                            rainToday = data.rainToday,
                            isDark = isDark,
                            size = 56.dp
                        )
                        Text(
                            text = data.currentWeatherText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // 当前温度
                    Text(
                        text = "${fmtTemp(data.currentTempC)}°",
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 52.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // 温度范围
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "温度",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${fmtTemp(data.minTempC)}℃ ~ ${fmtTemp(data.maxTempC)}℃",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // 出行提醒
                if (data.reminder.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.sm))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            .padding(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TipsAndUpdates,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = data.reminder,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * 天气动画图标
 */
@Composable
private fun WeatherAnimatedIconLarge(
    weatherText: String,
    rainToday: Boolean,
    isDark: Boolean,
    size: androidx.compose.ui.unit.Dp = 64.dp
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
            icon = Icons.Outlined.CloudQueue
            iconColor = if (isDark) Color(0xFF90A4AE) else Color(0xFF607D8B)
        }
        else -> {
            icon = Icons.Filled.WbSunny
            iconColor = if (isDark) Color(0xFFFFB74D) else Color(0xFFFFA000)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "weatherIcon")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconPulse"
    )

    Icon(
        imageVector = icon,
        contentDescription = weatherText,
        tint = iconColor,
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = pulse
                scaleY = pulse
            }
    )
}

/**
 * 下雨提醒卡片
 */
@Composable
private fun WeatherRainAlert(data: CampusService.WeatherSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.md),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Umbrella,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "今日有雨",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (data.rainPeriodText != null) {
                    Text(
                        text = data.rainPeriodText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "记得带伞",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * 天气信息卡片
 */
@Composable
private fun WeatherInfoCard(data: CampusService.WeatherSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.md),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WeatherInfoItem(
                icon = Icons.Filled.Air,
                label = "风速",
                value = String.format("%.1f m/s", data.windSpeedMps)
            )
            WeatherInfoItem(
                icon = Icons.Filled.WbTwilight,
                label = "日出",
                value = data.sunrise
            )
            WeatherInfoItem(
                icon = Icons.Filled.NightsStay,
                label = "日落",
                value = data.sunset
            )
        }
    }
}

/**
 * 天气信息单项
 */
@Composable
private fun WeatherInfoItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 穿衣建议卡片
 */
@Composable
private fun WeatherDressingAdvice(data: CampusService.WeatherSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.md),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Icon(
                    imageVector = Icons.Filled.Checkroom,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "穿衣建议",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 体感温度 + 舒适度
            if (data.temperatureLevel.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Text(
                        text = "体感 ${fmtTemp(data.feelTempC)}℃",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "·",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = data.temperatureLevel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if (data.coreAdvice.isNotBlank()) {
                Text(
                    text = data.coreAdvice,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

/**
 * 温馨提示区域
 */
@Composable
private fun WeatherTipsSection(tips: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.md),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Icon(
                    imageVector = Icons.Filled.TipsAndUpdates,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "温馨提醒",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            tips.forEach { tip ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    )
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

/**
 * 天气页面骨架屏
 */
@Composable
private fun WeatherPageSkeleton() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // 主卡片骨架
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radius.xl)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 120.dp, height = 16.dp)
                            .clip(RoundedCornerShape(Radius.sm))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 80.dp, height = 48.dp)
                            .clip(RoundedCornerShape(Radius.sm))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
                    )
                }
            }
        }

        // 信息行骨架
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radius.md)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(3) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
                            )
                            Box(
                                modifier = Modifier
                                    .size(width = 48.dp, height = 14.dp)
                                    .clip(RoundedCornerShape(Radius.xs))
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 天气页面错误状态
 */
@Composable
private fun WeatherPageError(error: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = "天气加载失败",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 天气页面空状态
 */
@Composable
private fun WeatherPageEmpty() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.WbCloudy,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = "暂无天气数据",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = "请检查网络连接",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==================== 工具函数 ====================
private fun fmtTemp(v: Double): String = kotlin.math.round(v).toInt().toString()
