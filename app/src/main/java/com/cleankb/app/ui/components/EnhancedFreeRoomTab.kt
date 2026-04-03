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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cleankb.app.data.CampusService
import com.cleankb.app.ui.theme.Radius
import com.cleankb.app.ui.theme.Spacing

/**
 * 空教室页面 - 简洁设计
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedFreeRoomTab(
    data: CampusService.FreeRoomSchedule?,
    selectedBuilding: String?,
    buildingOrder: List<String>,
    onBuildingOrderChange: (List<String>) -> Unit,
    onBuildingSelect: (String?) -> Unit
) {
    if (data == null) {
        return FreeRoomEmptyState()
    }

    val recommendedKey = data.recommended?.let { "${it.building}-${it.room}" }
    val remainingRooms = data.rooms
        .filterNot { "${it.building}-${it.room}" == recommendedKey }
        .take(12)

    var showSortSheet by remember { mutableStateOf(false) }
    var currentOrder by remember { mutableStateOf(buildingOrder) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(bottom = Spacing.xxl)
    ) {
        // 楼栋筛选器
        item {
            BuildingFilterSection(
                selectedBuilding = selectedBuilding,
                buildingOrder = buildingOrder,
                onBuildingSelect = onBuildingSelect,
                onSortClick = {
                    currentOrder = buildingOrder
                    showSortSheet = true
                }
            )
        }

        // 推荐教室
        item {
            if (data.recommended == null) {
                EmptyRecommendationCard(
                    buildingLabel = selectedBuilding ?: data.lastCourseBuilding
                )
            } else {
                RecommendedRoomCard(room = data.recommended)
            }
        }

        // 候选教室列表
        if (remainingRooms.isNotEmpty()) {
            item {
                CandidateRoomsHeader(count = remainingRooms.size)
            }

            items(remainingRooms, key = { "${it.building}-${it.room}" }) { room ->
                CandidateRoomCard(room = room)
            }
        }
    }

    // 楼栋排序底部弹窗
    if (showSortSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            BuildingSortSheet(
                buildings = currentOrder,
                onMoveUp = { index ->
                    if (index > 0) {
                        val newList = currentOrder.toMutableList()
                        val item = newList.removeAt(index)
                        newList.add(index - 1, item)
                        currentOrder = newList
                        onBuildingOrderChange(newList)
                    }
                },
                onMoveDown = { index ->
                    if (index < currentOrder.size - 1) {
                        val newList = currentOrder.toMutableList()
                        val item = newList.removeAt(index)
                        newList.add(index + 1, item)
                        currentOrder = newList
                        onBuildingOrderChange(newList)
                    }
                },
                onClose = { showSortSheet = false }
            )
        }
    }
}

/**
 * 楼栋筛选区域
 */
@Composable
private fun BuildingFilterSection(
    selectedBuilding: String?,
    buildingOrder: List<String>,
    onBuildingSelect: (String?) -> Unit,
    onSortClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            contentPadding = PaddingValues(end = Spacing.sm)
        ) {
            items(buildingOrder, key = { it }) { building ->
                BuildingFilterChip(
                    label = building,
                    selected = selectedBuilding == building,
                    onClick = { onBuildingSelect(building) }
                )
            }
        }

        // 排序按钮
        IconButton(
            onClick = onSortClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Tune,
                contentDescription = "调整楼栋顺序",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 楼栋筛选芯片
 */
@Composable
private fun BuildingFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        },
        label = "containerColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        label = "contentColor"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "chipScale"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(Radius.lg))
            .background(containerColor)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(Radius.lg)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 楼栋排序底部弹窗内容
 */
@Composable
private fun BuildingSortSheet(
    buildings: List<String>,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // 标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "调整楼栋顺序",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "关闭"
                )
            }
        }

        // 提示文字
        Text(
            text = "点击箭头按钮调整楼栋的排列顺序",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.lg)
        )

        // 楼栋列表
        LazyColumn(
            modifier = Modifier.padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            itemsIndexed(buildings) { index, building ->
                BuildingSortItem(
                    name = building,
                    index = index + 1,
                    canMoveUp = index > 0,
                    canMoveDown = index < buildings.size - 1,
                    onMoveUp = { onMoveUp(index) },
                    onMoveDown = { onMoveDown(index) }
                )
            }
        }
    }
}

/**
 * 楼栋排序列表项
 */
@Composable
private fun BuildingSortItem(
    name: String,
    index: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val shape = RoundedCornerShape(Radius.md)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 序号 + 名称
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$index",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 上下移动按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = "上移",
                        modifier = Modifier.size(18.dp),
                        tint = if (canMoveUp) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        }
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDownward,
                        contentDescription = "下移",
                        modifier = Modifier.size(18.dp),
                        tint = if (canMoveDown) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        }
                    )
                }
            }
        }
    }
}

/**
 * 推荐教室卡片 - 紧凑版
 */
@Composable
private fun RecommendedRoomCard(room: CampusService.FreeRoomItem) {
    val shape = RoundedCornerShape(Radius.lg)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = shape,
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：教室信息（占据主要空间）
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 推荐标签 + 教室名
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = room.room,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1
                    )
                }

                // 楼栋
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
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                        Text(
                            text = compactBuilding,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                }
            }

            // 右侧：空闲节次
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "空闲节次",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
                Text(
                    text = room.freePeriods.joinToString(" "),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 空闲节次流式标签
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeriodFlow(periods: List<Int>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        periods.forEach { period ->
            val (container, content) = getPeriodColors(period)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.full))
                    .background(container)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$period",
                    style = MaterialTheme.typography.labelSmall,
                    color = content,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * 空推荐卡片
 */
@Composable
private fun EmptyRecommendationCard(buildingLabel: String) {
    val shape = RoundedCornerShape(Radius.lg)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(Spacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Icon(
                imageVector = Icons.Filled.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = if (buildingLabel.isBlank()) "暂无空教室" else "$buildingLabel 暂无空教室",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 候选教室标题
 */
@Composable
private fun CandidateRoomsHeader(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            Text(
                text = "候选教室",
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

/**
 * 候选教室卡片 - 简洁版
 */
@Composable
private fun CandidateRoomCard(room: CampusService.FreeRoomItem) {
    val tone = availabilityTone(room.runLength)
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 2.dp,
                shape = shape,
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = { }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 教室图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(Radius.md))
                    .background(tone.container),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MeetingRoom,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = tone.content
                )
            }

            // 教室信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = room.room,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

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

            // 空闲节次
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "空闲",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = formatPeriods(room.freePeriods),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 空教室页面空状态
 */
@Composable
private fun FreeRoomEmptyState() {
    val infiniteTransition = rememberInfiniteTransition(label = "emptyState")

    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(100.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = pulseAlpha),
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0f)
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            )

            Icon(
                imageVector = Icons.Filled.MeetingRoom,
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .graphicsLayer { translationY = floatY },
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text = "暂无空教室数据",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        Text(
            text = "请检查网络连接或学号设置",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ==================== 工具函数 ====================

private data class AvailabilityTone(
    val label: String,
    val container: Color,
    val content: Color
)

@Composable
private fun availabilityTone(runLength: Int): AvailabilityTone {
    return when {
        runLength >= 4 -> AvailabilityTone(
            label = "超稳",
            container = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
            content = MaterialTheme.colorScheme.tertiary
        )
        runLength >= 2 -> AvailabilityTone(
            label = "可冲",
            container = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
            content = MaterialTheme.colorScheme.secondary
        )
        else -> AvailabilityTone(
            label = "短暂",
            container = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
            content = MaterialTheme.colorScheme.error
        )
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

private fun formatPeriods(periods: List<Int>): String {
    if (periods.isEmpty()) return "-"
    return periods.joinToString(" ")
}

private fun compactFreeRoomBuilding(room: String, building: String): String {
    var compact = building.trim().replace(Regex("\\s+"), " ")
    // 先处理中文括号
    compact = compact.replace("（", "(").replace("）", ")")
    // 简化楼栋名称：西校区(东)0#楼 -> 0#楼，西校区(西)3号楼 -> 3#楼
    compact = compact.replace(Regex("西校区\\s*\\(东\\)\\s*(\\d+)\\s*[#号]?楼"), "$1#楼")
    compact = compact.replace(Regex("西校区\\s*\\(西\\)\\s*(\\d+)\\s*[#号]?楼"), "$1#楼")
    if (compact.isBlank()) return ""
    // 去除与教室名重复的部分
    val labels = listOf("弘善楼", "弘毅楼", "弘德楼", "合教楼", "合四", "合五")
    labels.forEach { label ->
        if (label.isNotBlank() && room.contains(label) && compact.contains(label)) {
            compact = compact.replace(label, " ").replace(Regex("\\s+"), " ").trim()
        }
    }
    return compact
}
