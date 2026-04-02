package com.cleankb.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.cleankb.app.ui.theme.Radius
import com.cleankb.app.ui.theme.Spacing

@Composable
private fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    return Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        ),
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )
}

@Composable
private fun ShimmerBlock(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(Radius.sm)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(shimmerBrush())
    )
}

@Composable
fun EnhancedLoadingState(
    message: String = "加载中...",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        repeat(4) {
            CourseCardSkeleton()
        }
    }
}

@Composable
private fun WeatherCardSkeleton() {
    val shape = RoundedCornerShape(Radius.xl)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(Spacing.lg)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBlock(
                    modifier = Modifier
                        .width(120.dp)
                        .height(16.dp)
                )
                ShimmerBlock(
                    modifier = Modifier
                        .size(32.dp),
                    shape = RoundedCornerShape(Radius.full)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ShimmerBlock(
                        modifier = Modifier
                            .width(80.dp)
                            .height(40.dp)
                    )
                    ShimmerBlock(
                        modifier = Modifier
                            .width(60.dp)
                            .height(14.dp)
                    )
                }
                ShimmerBlock(
                    modifier = Modifier
                        .size(64.dp),
                    shape = RoundedCornerShape(Radius.md)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(4) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ShimmerBlock(
                            modifier = Modifier.size(16.dp),
                            shape = RoundedCornerShape(Radius.full)
                        )
                        ShimmerBlock(
                            modifier = Modifier
                                .width(32.dp)
                                .height(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseCardSkeleton() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            ShimmerBlock(
                modifier = Modifier.size(10.dp),
                shape = RoundedCornerShape(Radius.full)
            )
            ShimmerBlock(
                modifier = Modifier
                    .width(2.dp)
                    .height(32.dp)
            )
        }

        val shape = RoundedCornerShape(Radius.lg)
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(Spacing.md)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                ShimmerBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(18.dp)
                )
                ShimmerBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.35f)
                        .height(14.dp)
                )
                ShimmerBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(14.dp)
                )
            }
        }
    }
}

@Composable
fun WeekSkeletonLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        repeat(5) {
            WeekDaySkeleton()
        }
    }
}

@Composable
private fun WeekDaySkeleton() {
    val shape = RoundedCornerShape(Radius.md)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(Spacing.md)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBlock(
                    modifier = Modifier
                        .width(48.dp)
                        .height(16.dp)
                )
                ShimmerBlock(
                    modifier = Modifier
                        .width(36.dp)
                        .height(12.dp)
                )
            }

            repeat(2) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ShimmerBlock(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(14.dp)
                    )
                    ShimmerBlock(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FreeRoomSkeletonLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            repeat(4) {
                ShimmerBlock(
                    modifier = Modifier
                        .width(72.dp)
                        .height(32.dp),
                    shape = RoundedCornerShape(Radius.full)
                )
            }
        }

        val shape = RoundedCornerShape(Radius.lg)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                .padding(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ShimmerBlock(
                        modifier = Modifier
                            .width(32.dp)
                            .height(10.dp)
                    )
                    ShimmerBlock(
                        modifier = Modifier
                            .width(80.dp)
                            .height(16.dp)
                    )
                    ShimmerBlock(
                        modifier = Modifier
                            .width(120.dp)
                            .height(12.dp)
                    )
                }
                ShimmerBlock(
                    modifier = Modifier
                        .width(80.dp)
                        .height(32.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(Spacing.md)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                repeat(4) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.xs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            ShimmerBlock(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(14.dp)
                            )
                            ShimmerBlock(
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(12.dp)
                            )
                        }
                        ShimmerBlock(
                            modifier = Modifier
                                .width(60.dp)
                                .height(14.dp)
                        )
                    }
                }
            }
        }
    }
}
