package com.cleankb.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.cleankb.app.data.CampusService
import com.cleankb.app.ui.theme.PrimaryGradient
import com.cleankb.app.ui.theme.Radius
import com.cleankb.app.ui.theme.Spacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EnhancedFreeRoomTab(
    data: CampusService.FreeRoomSchedule?,
    selectedBuilding: String?,
    buildingOrder: List<String>,
    onBuildingOrderChange: (List<String>) -> Unit,
    onBuildingSelect: (String?) -> Unit
) {
    if (data == null) {
        return AnimatedEmptyState(
            icon = Icons.Filled.SearchOff,
            title = "暂无空教室数据",
            subtitle = "请检查网络连接或学号设置"
        )
    }

    var showReorderMode by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(bottom = Spacing.xxl)
    ) {
        // 时间状态提示
        item {
            TimeStatusCard(
                currentWeek = data.currentWeek,
                weekday = data.weekday,
                season = data.season,
                timeStatus = data.timeStatus,
                targetPeriod = data.targetPeriod
            )
        }

        // 楼栋选择器
        item {
            BuildingSelectorSection(
                anchorBuilding = data.anchorBuilding,
                selectedBuilding = selectedBuilding,
                buildingOrder = buildingOrder,
                showReorderMode = showReorderMode,
                onBuildingSelect = onBuildingSelect,
                onToggleReorderMode = { showReorderMode = !showReorderMode }
            )
        }

        // 拖动排序模式提示
        if (showReorderMode) {
            item {
                ReorderHintCard(
                    onDismiss = { showReorderMode = false }
                )
            }
        }

        // 拖动排序列表
        if (showReorderMode) {
            item {
                ReorderableBuildingList(
                    buildingOrder = buildingOrder,
                    anchorBuilding = data.anchorBuilding,
                    onOrderChange = onBuildingOrderChange
                )
            }
        }

        // 推荐教室
        item {
            val r = data.recommended
            if (r == null) {
                val msg = if (selectedBuilding == null) {
                    "当前没找到符合规则的空教室"
                } else {
                    "${selectedBuilding} 当前没有符合规则的空教室"
                }
                AppCard {
                    AnimatedEmptyState(
                        icon = Icons.Filled.SearchOff,
                        title = msg,
                        subtitle = "试试其他楼栋？"
                    )
                }
            } else {
                EnhancedRecommendedRoomCard(room = r)
            }
        }

        // 其他空教室列表
        if (data.rooms.isNotEmpty()) {
            item {
                SectionDivider(
                    title = "其他可用教室",
                    count = data.rooms.size
                )
            }

            val compactRooms = data.rooms.take(6)
            items(compactRooms, key = { it.room }) { room ->
                EnhancedRoomCard(room = room)
            }
        }
    }
}

@Composable
private fun TimeStatusCard(
    currentWeek: String,
    weekday: Int,
    season: String,
    timeStatus: String,
    targetPeriod: Int
) {
    AppCard(
        gradientColors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left accent bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                            )
                        )
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "第${currentWeek}周 · ${weekdayName(weekday)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = timeStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Season badge with icon
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.sm))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = Spacing.sm, vertical = 4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WbSunny,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = season,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                // Period badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.sm))
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f))
                        .padding(horizontal = Spacing.sm, vertical = 4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "第${targetPeriod}节",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BuildingSelectorSection(
    anchorBuilding: String,
    selectedBuilding: String?,
    buildingOrder: List<String>,
    showReorderMode: Boolean,
    onBuildingSelect: (String?) -> Unit,
    onToggleReorderMode: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "选择楼栋",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.sm))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = onToggleReorderMode
                    )
                    .padding(horizontal = Spacing.sm, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.DragHandle,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (showReorderMode) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = if (showReorderMode) "完成排序" else "排序偏好",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (showReorderMode) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            val displayBuildings = buildingOrder.take(8)
            displayBuildings.forEach { building ->
                val isSelected = if (building == anchorBuilding) {
                    selectedBuilding == null || selectedBuilding == anchorBuilding
                } else {
                    selectedBuilding == building
                }

                BuildingFilterChip(
                    label = building,
                    isAnchor = building == anchorBuilding,
                    isSelected = isSelected,
                    onClick = {
                        if (building == anchorBuilding) {
                            onBuildingSelect(null)
                        } else {
                            onBuildingSelect(building)
                        }
                    }
                )
            }

            if (buildingOrder.size > 8) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.full))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = Spacing.md, vertical = 8.dp)
                ) {
                    Text(
                        text = "+${buildingOrder.size - 8}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BuildingFilterChip(
    label: String,
    isAnchor: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected && isAnchor -> MaterialTheme.colorScheme.primary
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(200),
        label = "chipBg"
    )

    val textColor by animateColorAsState(
        targetValue = when {
            isSelected && isAnchor -> MaterialTheme.colorScheme.onPrimary
            isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "chipText"
    )

    val elevation by animateDpAsState(
        targetValue = if (isSelected) 4.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "chipElevation"
    )

    Box(
        modifier = Modifier
            .shadow(elevation, RoundedCornerShape(Radius.full))
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 1.5.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        ),
                        shape = RoundedCornerShape(Radius.full)
                    )
                } else Modifier
            )
            .clip(RoundedCornerShape(Radius.full))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            )
            .padding(horizontal = Spacing.md, vertical = 8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isAnchor) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = "最近楼栋",
                    modifier = Modifier.size(12.dp),
                    tint = textColor
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor
            )
        }
    }
}

@Composable
private fun ReorderHintCard(onDismiss: () -> Unit) {
    AppCard(
        gradientColors = listOf(
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.TouchApp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "长按并拖动楼栋名称调整顺序",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "关闭提示",
                modifier = Modifier
                    .size(20.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = onDismiss
                    ),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun ReorderableBuildingList(
    buildingOrder: List<String>,
    anchorBuilding: String,
    onOrderChange: (List<String>) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var draggingKey by remember { mutableStateOf<String?>(null) }

    AppCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            buildingOrder.forEachIndexed { index, building ->
                val isDragging = draggingKey == building
                val isAnchor = building == anchorBuilding

                ReorderableBuildingItem(
                    building = building,
                    index = index,
                    isAnchor = isAnchor,
                    isDragging = isDragging,
                    onDragStart = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        draggingKey = building
                    },
                    onDragEnd = { draggingKey = null },
                    onMoveUp = {
                        if (index > 0) {
                            val newOrder = buildingOrder.toMutableList()
                            val item = newOrder.removeAt(index)
                            newOrder.add(index - 1, item)
                            onOrderChange(newOrder)
                        }
                    },
                    onMoveDown = {
                        if (index < buildingOrder.size - 1) {
                            val newOrder = buildingOrder.toMutableList()
                            val item = newOrder.removeAt(index)
                            newOrder.add(index + 1, item)
                            onOrderChange(newOrder)
                        }
                    }
                )

                if (index < buildingOrder.size - 1) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun ReorderableBuildingItem(
    building: String,
    index: Int,
    isAnchor: Boolean,
    isDragging: Boolean,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 0.dp,
        label = "itemElevation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "itemScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 1f else 0f)
            .shadow(elevation, RoundedCornerShape(Radius.sm))
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(
                if (isDragging) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(20.dp)
            )

            if (isAnchor) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = "最近楼栋",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = building,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isAnchor) FontWeight.SemiBold else FontWeight.Normal
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(Radius.sm))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = onMoveUp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "上移",
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = 180f },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(Radius.sm))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = onMoveDown
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "下移",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(Radius.sm))
                    .pointerInput(building) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart() },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                            onDrag = { _, _ -> }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.DragHandle,
                    contentDescription = "拖动排序",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EnhancedRecommendedRoomCard(room: CampusService.FreeRoomItem) {
    val elevation by animateDpAsState(
        targetValue = 6.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardElevation"
    )

    AppCard(
        modifier = Modifier.shadow(
            elevation = elevation,
            shape = RoundedCornerShape(Radius.lg),
            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        ),
        gradientColors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // Header row with badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "推荐教室",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Availability indicator
                val periodCount = room.freePeriods.size
                val availabilityColor = when {
                    periodCount >= 4 -> MaterialTheme.colorScheme.tertiary
                    periodCount >= 2 -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.error
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.sm))
                        .background(availabilityColor.copy(alpha = 0.15f))
                        .padding(horizontal = Spacing.sm, vertical = 2.dp)
                ) {
                    Text(
                        text = "${periodCount}节空闲",
                        style = MaterialTheme.typography.labelSmall,
                        color = availabilityColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Main content
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = room.room,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    val compactBuilding = compactFreeRoomBuilding(room.room, room.building)
                    if (compactBuilding.isNotBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Home,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                            Text(
                                text = compactBuilding,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                FreePeriodsBlock(periods = room.freePeriods)
            }
        }
    }
}

@Composable
private fun EnhancedRoomCard(room: CampusService.FreeRoomItem) {
    val isPressed = remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed.value) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardScale"
    )

    AppCard(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = room.room,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    // Availability dots
                    val periodCount = room.freePeriods.size
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(minOf(periodCount, 3)) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        when {
                                            periodCount >= 4 -> MaterialTheme.colorScheme.tertiary
                                            periodCount >= 2 -> MaterialTheme.colorScheme.secondary
                                            else -> MaterialTheme.colorScheme.error
                                        }
                                    )
                            )
                        }
                    }
                }
                val compactBuilding = compactFreeRoomBuilding(room.room, room.building)
                if (compactBuilding.isNotBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = compactBuilding,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            FreePeriodsBadges(periods = room.freePeriods)
        }
    }
}

@Composable
private fun FreePeriodsBlock(periods: List<Int>) {
    Column(
        modifier = Modifier.width(80.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "空闲节次",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatPeriods(periods),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FreePeriodsBadges(periods: List<Int>) {
    FlowRow(
        modifier = Modifier.width(100.dp),
        horizontalArrangement = Arrangement.End,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        maxItemsInEachRow = 3
    ) {
        periods.take(5).forEach { period ->
            val (bgColor, textColor) = getPeriodColors(period)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.xs))
                    .background(bgColor)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$period",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            }
        }
        if (periods.size > 5) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.xs))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "+${periods.size - 5}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun getPeriodColors(period: Int): Pair<Color, Color> {
    return when (period) {
        in 1..2 -> Pair(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.primary
        )
        in 3..4 -> Pair(
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.secondary
        )
        in 5..6 -> Pair(
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.tertiary
        )
        in 7..8 -> Pair(
            MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.error
        )
        else -> Pair(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
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

private fun formatPeriods(periods: List<Int>): String {
    if (periods.isEmpty()) return "-"
    return periods.joinToString(" ")
}

private fun compactFreeRoomBuilding(room: String, building: String): String {
    var compact = building.trim().replace(Regex("\\s+"), " ")
    if (compact.isBlank()) return ""
    val duplicateLabels = listOf(
        "弘善楼",
        "弘毅楼",
        "弘德楼",
        "合教楼",
        "合四",
        "合五"
    ) + Regex("\\d+号楼").findAll(compact).map { it.value }.toList()
    duplicateLabels.distinct().forEach { label ->
        if (label.isNotBlank() && room.contains(label) && compact.contains(label)) {
            compact = compact.replace(label, " ").replace(Regex("\\s+"), " ").trim()
        }
    }
    return compact
}

@Composable
private fun SectionDivider(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left accent line
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(Radius.full))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                .padding(horizontal = Spacing.sm, vertical = 2.dp)
        ) {
            Text(
                text = "$count 间",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
