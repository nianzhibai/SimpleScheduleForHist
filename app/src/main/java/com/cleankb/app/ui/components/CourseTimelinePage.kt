package com.cleankb.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cleankb.app.data.CampusService
import com.cleankb.app.ui.theme.Radius
import com.cleankb.app.ui.theme.Spacing

/**
 * 今日课表完整页面
 *
 * 设计特点：
 * - 时间线布局，清晰展示课程顺序
 * - 左侧时间指示器区分进行中/已完成/未开始
 * - 课程卡片带渐变背景和按压动画
 * - 当前课程高亮显示
 */
@Composable
fun TodayCoursePage(
    data: CampusService.TodaySchedule?,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    if (data == null) {
        TodayCoursePageEmpty()
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // 页面头部
        item {
            TodayCourseHeader(
                currentWeek = data.currentWeek,
                weekday = data.weekday,
                courseCount = data.courses.size
            )
        }

        if (data.courses.isEmpty()) {
            item {
                TodayCourseEmpty()
            }
        } else {
            itemsIndexed(
                items = data.courses,
                key = { index, course -> "${course.name}-${course.section}-${course.location}-${course.beginTime}" }
            ) { index, course ->
                val isCurrent = isCurrentCourse(course)
                val isPast = isPastCourse(course)

                CourseTimelineItem(
                    course = course,
                    index = index,
                    isDark = isDark,
                    isCurrent = isCurrent,
                    isPast = isPast,
                    isLast = index == data.courses.lastIndex
                )
            }
        }

        // 底部留白
        item {
            Spacer(modifier = Modifier.height(Spacing.xxl))
        }
    }
}

/**
 * 今日课表页面头部
 */
@Composable
private fun TodayCourseHeader(
    currentWeek: String,
    weekday: Int,
    courseCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "今天课程",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "第${currentWeek}周 · ${weekdayName(weekday)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (courseCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.full))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs)
                ) {
                    Text(
                        text = "${courseCount}门课",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

/**
 * 课程时间线单项
 *
 * 布局：
 * ├── 时间指示器列（24dp）
 * │   ├── 圆点（当前课程放大+发光）
 * │   └── 连接线（已完成变淡）
 * └── 课程卡片（带渐变背景）
 */
@Composable
private fun CourseTimelineItem(
    course: CampusService.CourseItem,
    index: Int,
    isDark: Boolean,
    isCurrent: Boolean,
    isPast: Boolean,
    isLast: Boolean
) {
    val colors = courseTimelineColors(isDark)
    val cardColor = colors[index % colors.size]

    // 卡片透明度（已完成课程降低）
    val cardAlpha by animateFloatAsState(
        targetValue = if (isPast) 0.55f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cardAlpha"
    )

    // 按压动画
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // 时间指示器
        TimelineIndicator(
            index = index,
            isCurrent = isCurrent,
            isPast = isPast,
            isLast = isLast,
            color = cardColor
        )

        // 课程卡片
        TimelineCourseCard(
            course = course,
            cardColor = cardColor,
            isCurrent = isCurrent,
            isPast = isPast,
            scale = scale,
            alpha = cardAlpha,
            interactionSource = interactionSource
        )
    }
}

/**
 * 时间线指示器
 *
 * 包含：
 * - 顶部圆点（当前课程有脉冲动画）
 * - 连接线（渐变消失效果）
 */
@Composable
private fun TimelineIndicator(
    index: Int,
    isCurrent: Boolean,
    isPast: Boolean,
    isLast: Boolean,
    color: Color
) {
    val dotColor by animateColorAsState(
        targetValue = when {
            isCurrent -> MaterialTheme.colorScheme.primary
            isPast -> color.copy(alpha = 0.3f)
            else -> color
        },
        label = "dotColor"
    )

    val dotSize by animateDpAsState(
        targetValue = if (isCurrent) 16.dp else 10.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dotSize"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(24.dp)
    ) {
        // 圆点
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(dotSize)
        ) {
            // 外圈光晕（当前课程）
            if (isCurrent) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 0.7f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseAlpha"
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .alpha(pulseAlpha)
                        .clip(CircleShape)
                        .background(dotColor.copy(alpha = 0.3f))
                )
            }

            // 中心圆点
            Box(
                modifier = Modifier
                    .size(dotSize - 2.dp)
                    .shadow(
                        elevation = if (isCurrent) 4.dp else 0.dp,
                        shape = CircleShape,
                        spotColor = dotColor
                    )
                    .clip(CircleShape)
                    .background(dotColor),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
        }

        // 连接线
        if (!isLast) {
            val lineAlpha by animateFloatAsState(
                targetValue = if (isPast) 0.2f else 0.5f,
                label = "lineAlpha"
            )

            Canvas(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
            ) {
                val gradient = Brush.verticalGradient(
                    colors = listOf(
                        dotColor.copy(alpha = lineAlpha),
                        dotColor.copy(alpha = lineAlpha * 0.5f)
                    )
                )
                drawLine(
                    brush = gradient,
                    start = Offset(size.width / 2, 0f),
                    end = Offset(size.width / 2, size.height),
                    strokeWidth = size.width,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * 时间线课程卡片
 */
@Composable
private fun TimelineCourseCard(
    course: CampusService.CourseItem,
    cardColor: Color,
    isCurrent: Boolean,
    isPast: Boolean,
    scale: Float,
    alpha: Float,
    interactionSource: MutableInteractionSource
) {
    val shape = RoundedCornerShape(Radius.lg)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (isCurrent) 8.dp else 2.dp,
                shape = shape,
                spotColor = cardColor.copy(alpha = 0.4f)
            )
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = { }
            )
            .alpha(alpha),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(cardColor, cardColor.copy(alpha = 0.7f))
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                // 课程名称 + 状态标签
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = course.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (isCurrent) {
                        CurrentCourseBadge()
                    } else if (isPast) {
                        PastCourseBadge()
                    }
                }

                // 节次和时间
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sectionText = compactSectionText(course.section)
                    val timeText = formatCourseTime(course.beginTime, course.endTime)

                    if (sectionText.isNotBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "第${sectionText}节",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (timeText.isNotBlank()) {
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 地点和老师
                val locationText = displayLocation(course.location)
                if (locationText.isNotBlank() || course.teacher.isNotBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (locationText.isNotBlank()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = "教室",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = locationText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (course.teacher.isNotBlank()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = "教师",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = course.teacher,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 当前课程标签
 */
@Composable
private fun CurrentCourseBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.full))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            .padding(horizontal = Spacing.sm, vertical = 2.dp)
    ) {
        Text(
            text = "进行中",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * 已完成课程标签
 */
@Composable
private fun PastCourseBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.full))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = Spacing.sm, vertical = 2.dp)
    ) {
        Text(
            text = "已结束",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Normal
        )
    }
}

/**
 * 今日课表空状态
 */
@Composable
private fun TodayCourseEmpty() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Weekend,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = "今天没课",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = "好好休息，享受自由时光",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TodayCoursePageEmpty() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Schedule,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = "暂无课表数据",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = "请检查网络连接或学号设置",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==================== 工具函数 ====================
@Composable
private fun courseTimelineColors(isDark: Boolean): List<Color> {
    return if (isDark) {
        listOf(
            Color(0xFF2D3556),
            Color(0xFF3D3520),
            Color(0xFF1D3A2E),
            Color(0xFF3D2535),
            Color(0xFF2D3056),
            Color(0xFF1D3A38),
            Color(0xFF3D2525),
            Color(0xFF1D2D4A)
        )
    } else {
        listOf(
            Color(0xFFEEF2FF),
            Color(0xFFFEF3C7),
            Color(0xFFECFDF5),
            Color(0xFFFCE7F3),
            Color(0xFFE0E7FF),
            Color(0xFFCCFBF1),
            Color(0xFFFEE2E2),
            Color(0xFFDBEAFE)
        )
    }
}

private fun weekdayName(weekday: Int): String {
    return when (weekday) {
        1 -> "周一"
        2 -> "周二"
        3 -> "周三"
        4 -> "周四"
        5 -> "周五"
        6 -> "周六"
        7 -> "周日"
        else -> "周?"
    }
}

private fun formatCourseTime(begin: String, end: String): String {
    return when {
        begin.isNotBlank() && end.isNotBlank() -> "$begin - $end"
        begin.isNotBlank() -> begin
        end.isNotBlank() -> end
        else -> ""
    }
}

private fun compactSectionText(raw: String): String {
    val s = raw.trim()
    if (s.isBlank()) return ""
    val range = Regex("(\\d+)\\s*[-~到]\\s*(\\d+)").find(s)
    if (range != null) return "${range.groupValues[1]}-${range.groupValues[2]}"
    return Regex("\\d+").find(s)?.value.orEmpty()
}

private fun displayLocation(raw: String): String {
    val s = raw.trim()
    val m = Regex("\\[(.+)]").find(s)
    return if (m != null) m.groupValues[1].trim() else s
}

private fun isCurrentCourse(course: CampusService.CourseItem): Boolean {
    val now = java.time.LocalTime.now()
    val beginTime = parseTime(course.beginTime) ?: return false
    val endTime = parseTime(course.endTime) ?: return false
    return now.isAfter(beginTime) && now.isBefore(endTime)
}

private fun isPastCourse(course: CampusService.CourseItem): Boolean {
    val now = java.time.LocalTime.now()
    val endTime = parseTime(course.endTime) ?: return false
    return now.isAfter(endTime)
}

private fun parseTime(timeStr: String): java.time.LocalTime? {
    if (timeStr.isBlank()) return null
    return try {
        java.time.LocalTime.parse(timeStr)
    } catch (e: Exception) {
        null
    }
}
