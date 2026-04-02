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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cleankb.app.data.CampusService
import com.cleankb.app.ui.theme.Radius
import com.cleankb.app.ui.theme.Spacing

@Composable
fun TimelineCourseCard(
    course: CampusService.CourseItem,
    index: Int,
    isDark: Boolean,
    isCurrent: Boolean = false,
    isPast: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = courseCardColors(isDark)
    val cardColor = colors[index % colors.size]

    val cardAlpha by animateFloatAsState(
        targetValue = if (isPast) 0.6f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cardAlpha"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardScale"
    )

    val shape = RoundedCornerShape(Radius.lg)

    val clickModifier = if (onClick != null) {
        Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(cardAlpha),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        TimelineIndicator(
            index = index,
            isCurrent = isCurrent,
            isPast = isPast,
            color = cardColor
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .shadow(
                    elevation = if (isCurrent) 6.dp else 2.dp,
                    shape = shape,
                    spotColor = cardColor.copy(alpha = 0.3f)
                )
                .clip(shape)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            cardColor,
                            cardColor.copy(alpha = 0.6f)
                        )
                    )
                )
                .then(clickModifier)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                CourseCardHeader(
                    name = course.name,
                    isCurrent = isCurrent
                )

                CourseCardTime(
                    section = course.section,
                    beginTime = course.beginTime,
                    endTime = course.endTime
                )

                CourseCardDetails(
                    location = course.location,
                    teacher = course.teacher
                )
            }
        }
    }
}

@Composable
private fun TimelineIndicator(
    index: Int,
    isCurrent: Boolean,
    isPast: Boolean,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(24.dp)
    ) {
        val dotColor by animateColorAsState(
            targetValue = when {
                isCurrent -> MaterialTheme.colorScheme.primary
                isPast -> color.copy(alpha = 0.4f)
                else -> color
            },
            label = "dotColor"
        )

        val dotSize = if (isCurrent) 14.dp else 10.dp

        Box(
            modifier = Modifier
                .size(dotSize)
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

        Box(
            modifier = Modifier
                .width(2.dp)
                .height(32.dp)
                .background(
                    color = if (isPast) color.copy(alpha = 0.3f) else color.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(1.dp)
                )
        )
    }
}

@Composable
private fun CourseCardHeader(
    name: String,
    isCurrent: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (isCurrent) {
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
    }
}

@Composable
private fun CourseCardTime(
    section: String,
    beginTime: String,
    endTime: String
) {
    val timeText = formatCourseTime(beginTime, endTime)
    val sectionText = compactSectionText(section)

    if (sectionText.isNotBlank() || timeText.isNotBlank()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
    }
}

@Composable
private fun CourseCardDetails(
    location: String,
    teacher: String
) {
    val locationText = displayLocation(location)

    if (locationText.isNotBlank() || teacher.isNotBlank()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
            if (teacher.isNotBlank()) {
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
                        text = teacher,
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

@Composable
fun CompactCourseCard(
    course: CampusService.CourseItem,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = courseCardColors(isDark)
    val cardColor = colors.random()

    val shape = RoundedCornerShape(Radius.md)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardColor.copy(alpha = 0.5f))
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Text(
            text = course.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        val details = buildString {
            val section = compactSectionText(course.section)
            if (section.isNotBlank()) append("第${section}节")
            val loc = displayLocation(course.location)
            if (loc.isNotBlank()) {
                if (isNotBlank()) append(" · ")
                append(loc)
            }
            if (course.teacher.isNotBlank()) {
                if (isNotBlank()) append(" · ")
                append(course.teacher)
            }
        }
        if (details.isNotBlank()) {
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun courseCardColors(isDark: Boolean): List<Color> {
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
    val first = Regex("\\d+").find(s)?.value
    return first.orEmpty()
}

private fun displayLocation(raw: String): String {
    val s = raw.trim()
    val m = Regex("\\[(.+)]").find(s)
    if (m != null) return m.groupValues[1].trim()
    return s
}