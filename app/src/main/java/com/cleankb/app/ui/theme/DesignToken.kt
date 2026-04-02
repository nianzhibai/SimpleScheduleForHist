package com.cleankb.app.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ==================== 间距系统 ====================
// 基于 4dp 网格，提供统一的间距规范
object Spacing {
    val xs = 4.dp      // 最小间距：图标与文字
    val sm = 8.dp      // 小间距：列表项内边距
    val md = 16.dp     // 中等间距：卡片内边距
    val lg = 24.dp     // 大间距：区块间距
    val xl = 32.dp     // 超大间距：页面边距
    val xxl = 40.dp    // 最大间距：页面顶部
    val xxxl = 48.dp   // 特大间距：主要区块
}

// ==================== 圆角系统 ====================
// 统一的圆角规范，确保视觉一致性
object Radius {
    val xs = 4.dp      // 小元素：标签、徽章
    val sm = 8.dp      // 按钮、输入框
    val md = 12.dp     // 卡片
    val lg = 16.dp     // 大卡片、弹窗
    val xl = 20.dp     // 特殊卡片
    val full = 9999.dp // 胶囊形状
}

// ==================== 阴影系统 ====================
// 轻量级阴影，避免过重的视觉压迫感
object Elevation {
    val none = 0.dp
    val xs = 1.dp      // 微弱阴影：按钮
    val sm = 2.dp      // 轻阴影：卡片
    val md = 4.dp      // 中阴影：悬浮元素
    val lg = 8.dp      // 强阴影：弹窗
    val xl = 12.dp     // 最强阴影：模态框
}

// ==================== 字号系统 ====================
// 确保文字层次清晰
object FontSize {
    val xs = 10.sp
    val sm = 12.sp
    val md = 14.sp
    val lg = 16.sp
    val xl = 18.sp
    val xxl = 20.sp
    val xxxl = 24.sp
}

// ==================== 动画时长 ====================
// 统一的动画时长规范
object Duration {
    const val fast = 150L      // 快速：微交互
    const val normal = 250L    // 正常：状态切换
    const val slow = 350L      // 较慢：页面过渡
}

// ==================== 图标尺寸 ====================
object IconSize {
    val sm = 16.dp
    val md = 20.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

// ==================== 卡片尺寸 ====================
object CardSize {
    val minHeight = 80.dp
    val weatherHeight = 200.dp
    val courseMinHeight = 100.dp
}

// ==================== 点击区域 ====================
object TouchTarget {
    val minimum = 48.dp  // 无障碍最小点击区域
    val comfortable = 56.dp  // 舒适点击区域
}

// ==================== 动画参数 ====================
object AnimationSpec {
    const val bounceDampingRatio = 0.5f
    const val bouncyDampingRatio = 0.6f
    const val stiffStiffness = 200f
    const val mediumStiffness = 150f
    const val lowStiffness = 100f
}
