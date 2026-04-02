package com.cleankb.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cleankb.app.data.CampusService
import com.cleankb.app.ui.theme.Radius
import com.cleankb.app.ui.theme.Spacing

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

    val recommendedKey = data.recommended?.let { "${it.building}-${it.room}" }
    val remainingRooms = data.rooms
        .filterNot { "${it.building}-${it.room}" == recommendedKey }
        .take(12)

    var showReorderPanel by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(bottom = Spacing.xxxl)
    ) {
        item {
            FreeRoomOverviewCard(
                data = data,
                selectedBuilding = selectedBuilding
            )
        }

        item {
            BuildingFilterCard(
                anchorBuilding = data.anchorBuilding,
                selectedBuilding = selectedBuilding,
                buildingOrder = buildingOrder,
                showReorderPanel = showReorderPanel,
                onBuildingSelect = onBuildingSelect,
                onToggleReorder = { showReorderPanel = !showReorderPanel }
            )
        }

        if (showReorderPanel) {
            item {
                BuildingReorderCard(
                    buildingOrder = buildingOrder,
                    anchorBuilding = data.anchorBuilding,
                    onOrderChange = onBuildingOrderChange
                )
            }
        }

        item {
            if (data.recommended == null) {
                EmptyRecommendationCard(
                    buildingLabel = selectedBuilding ?: data.anchorBuilding
                )
            } else {
                RecommendedRoomCard(
                    room = data.recommended,
                    targetPeriod = data.targetPeriod
                )
            }
        }

        if (remainingRooms.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "候选教室",
                    subtitle = "按当前规则排序后的其他选择"
                )
            }

            items(remainingRooms, key = { "${it.building}-${it.room}" }) { room ->
                CandidateRoomCard(
                    room = room,
                    targetPeriod = data.targetPeriod
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FreeRoomOverviewCard(
    data: CampusService.FreeRoomSchedule,
    selectedBuilding: String?
) {
    val focusBuilding = selectedBuilding ?: data.anchorBuilding

    AppCard(
        gradientColors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Text(
                        text = "空教室推荐",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "第${data.currentWeek}周 · ${weekdayName(data.weekday)} · ${data.timeStatus}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                PillTag(
                    icon = Icons.Filled.AccessTime,
                    text = "第${data.targetPeriod}节",
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                if (focusBuilding.isNotBlank()) {
                    InfoChip(
                        icon = Icons.Filled.LocationOn,
                        label = "当前楼栋",
                        value = focusBuilding
                    )
                }
                InfoChip(
                    icon = Icons.Filled.WbSunny,
                    label = "作息",
                    value = data.season
                )
                InfoChip(
                    icon = Icons.Filled.AutoAwesome,
                    label = "推荐数",
                    value = "${data.rooms.size} 间"
                )
            }

            SummaryStrip(
                title = "你的空档",
                value = formatPeriods(data.freePeriods),
                accent = MaterialTheme.colorScheme.secondary
            )
            SummaryStrip(
                title = "你的有课节次",
                value = formatPeriods(data.busyPeriods),
                accent = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun BuildingFilterCard(
    anchorBuilding: String,
    selectedBuilding: String?,
    buildingOrder: List<String>,
    showReorderPanel: Boolean,
    onBuildingSelect: (String?) -> Unit,
    onToggleReorder: () -> Unit
) {
    AppCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "楼栋筛选",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "先看常用楼栋，再切换排序偏好",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TertiaryActionChip(
                    icon = Icons.AutoMirrored.Filled.Sort,
                    text = if (showReorderPanel) "收起排序" else "调整排序",
                    onClick = onToggleReorder
                )
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                item {
                    BuildingPill(
                        label = if (anchorBuilding.isBlank()) "智能推荐" else "最近楼栋",
                        supporting = if (anchorBuilding.isBlank()) "按规则推荐" else anchorBuilding,
                        selected = selectedBuilding == null,
                        highlighted = true,
                        onClick = { onBuildingSelect(null) }
                    )
                }

                items(buildingOrder, key = { it }) { building ->
                    BuildingPill(
                        label = building,
                        supporting = if (building == anchorBuilding) "当前最近" else "手动筛选",
                        selected = selectedBuilding == building,
                        highlighted = building == anchorBuilding,
                        onClick = { onBuildingSelect(building) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BuildingPill(
    label: String,
    supporting: String,
    selected: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    val containerColor = when {
        selected -> MaterialTheme.colorScheme.primary
        highlighted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }
    val contentColor = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        highlighted -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.lg))
            .background(containerColor)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = if (highlighted) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                },
                shape = RoundedCornerShape(Radius.lg)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
private fun BuildingReorderCard(
    buildingOrder: List<String>,
    anchorBuilding: String,
    onOrderChange: (List<String>) -> Unit
) {
    AppCard(
        gradientColors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(
                text = "楼栋排序",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "优先级越靠前，推荐时权重越高。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            buildingOrder.forEachIndexed { index, building ->
                ReorderRow(
                    index = index,
                    building = building,
                    isAnchor = building == anchorBuilding,
                    canMoveUp = index > 0,
                    canMoveDown = index < buildingOrder.lastIndex,
                    onMoveUp = {
                        if (index == 0) return@ReorderRow
                        val next = buildingOrder.toMutableList()
                        val item = next.removeAt(index)
                        next.add(index - 1, item)
                        onOrderChange(next)
                    },
                    onMoveDown = {
                        if (index == buildingOrder.lastIndex) return@ReorderRow
                        val next = buildingOrder.toMutableList()
                        val item = next.removeAt(index)
                        next.add(index + 1, item)
                        onOrderChange(next)
                    }
                )
            }
        }
    }
}

@Composable
private fun ReorderRow(
    index: Int,
    building: String,
    isAnchor: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column {
                Text(
                    text = building,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isAnchor) "当前最近楼栋" else "手动优先级",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            SmallActionButton(
                icon = Icons.Filled.ArrowUpward,
                enabled = canMoveUp,
                contentDescription = "上移",
                onClick = onMoveUp
            )
            SmallActionButton(
                icon = Icons.Filled.ArrowDownward,
                enabled = canMoveDown,
                contentDescription = "下移",
                onClick = onMoveDown
            )
        }
    }
}

@Composable
private fun SmallActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(Radius.sm))
            .background(
                if (enabled) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                }
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
            tint = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
            }
        )
    }
}

@Composable
private fun EmptyRecommendationCard(buildingLabel: String) {
    AppCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Icon(
                imageVector = Icons.Filled.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            )
            Text(
                text = if (buildingLabel.isBlank()) "当前没找到符合规则的空教室" else "$buildingLabel 暂时没有合适空教室",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "换一个楼栋，或者等下一节再查。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecommendedRoomCard(
    room: CampusService.FreeRoomItem,
    targetPeriod: Int
) {
    val tone = availabilityTone(room.runLength)

    AppCard(
        gradientColors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.52f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Text(
                        text = "推荐教室",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = room.room,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    val compactBuilding = compactFreeRoomBuilding(room.room, room.building)
                    if (compactBuilding.isNotBlank()) {
                        Text(
                            text = compactBuilding,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                        )
                    }
                }

                PillTag(
                    icon = Icons.Filled.AutoAwesome,
                    text = tone.label,
                    containerColor = tone.container,
                    contentColor = tone.content
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                InfoChip(
                    icon = Icons.Filled.AccessTime,
                    label = "起始节次",
                    value = "第${room.startPeriod}节"
                )
                InfoChip(
                    icon = Icons.Filled.Tune,
                    label = "连续空闲",
                    value = "${room.runLength} 节"
                )
                InfoChip(
                    icon = Icons.Filled.MeetingRoom,
                    label = "容量",
                    value = "${room.capacity} 人"
                )
            }

            SummaryStrip(
                title = if (room.startPeriod == targetPeriod) "现在可去" else "建议等待",
                value = if (room.startPeriod == targetPeriod) {
                    "当前就可以使用，连续空闲 ${room.runLength} 节"
                } else {
                    "从第${room.startPeriod}节开始空出，连续 ${room.runLength} 节"
                },
                accent = tone.content
            )

            PeriodFlow(periods = room.freePeriods)
        }
    }
}

@Composable
private fun CandidateRoomCard(
    room: CampusService.FreeRoomItem,
    targetPeriod: Int
) {
    val tone = availabilityTone(room.runLength)

    AppCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
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
                            tint = tone.content
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = room.room,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val compactBuilding = compactFreeRoomBuilding(room.room, room.building)
                        if (compactBuilding.isNotBlank()) {
                            Text(
                                text = compactBuilding,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (room.startPeriod == targetPeriod) "现在可用" else "第${room.startPeriod}节可用",
                        style = MaterialTheme.typography.labelMedium,
                        color = tone.content,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "连续 ${room.runLength} 节",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "容量 ${room.capacity} 人",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatPeriods(room.freePeriods),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun TertiaryActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.full))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PillTag(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.full))
            .background(containerColor)
            .padding(horizontal = Spacing.sm, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = contentColor
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.md))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(Radius.md)
            )
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SummaryStrip(
    title: String,
    value: String,
    accent: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = accent,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

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
                    .padding(horizontal = Spacing.sm, vertical = 6.dp)
            ) {
                Text(
                    text = "第${period}节",
                    style = MaterialTheme.typography.labelSmall,
                    color = content,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

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
