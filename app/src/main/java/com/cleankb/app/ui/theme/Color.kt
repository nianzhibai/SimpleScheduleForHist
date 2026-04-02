package com.cleankb.app.ui.theme

import androidx.compose.ui.graphics.Color

// ==================== 主色系统 (Indigo) ====================
// 主色调：靛蓝色系，传达学术、专业感
val Primary50 = Color(0xFFEEF2FF)   // 最浅 - 背景色
val Primary100 = Color(0xFFE0E7FF)  // 浅色容器
val Primary200 = Color(0xFFC7D2FE)  // 浅色边框
val Primary300 = Color(0xFFA5B4FC)  // 次要元素
val Primary400 = Color(0xFF818CF8)  // 交互元素
val Primary500 = Color(0xFF6366F1)  // 主色
val Primary600 = Color(0xFF4F46E5)  // 深色主色
val Primary700 = Color(0xFF4338CA)  // 按压态
val Primary800 = Color(0xFF3730A3)  // 深色文字
val Primary900 = Color(0xFF312E81)  // 最深

// ==================== 辅色系统 (Teal) ====================
// 辅色：青色系，用于次要操作和强调
val Secondary50 = Color(0xFFF0FDFA)
val Secondary100 = Color(0xFFCCFBF1)
val Secondary200 = Color(0xFF99F6E4)
val Secondary300 = Color(0xFF5EEAD4)
val Secondary400 = Color(0xFF2DD4BF)
val Secondary500 = Color(0xFF14B8A6)
val Secondary600 = Color(0xFF0D9488)
val Secondary700 = Color(0xFF0F766E)

// ==================== 中性色系统 ====================
// 灰度色阶，用于文字、背景、边框等
val Gray50 = Color(0xFFF9FAFB)
val Gray100 = Color(0xFFF3F4F6)   // 页面背景
val Gray200 = Color(0xFFE5E7EB)   // 卡片背景、分割线
val Gray300 = Color(0xFFD1D5DB)   // 边框
val Gray400 = Color(0xFF9CA3AF)   // 占位文字
val Gray500 = Color(0xFF6B7280)   // 次要文字
val Gray600 = Color(0xFF4B5563)   // 正文文字
val Gray700 = Color(0xFF374151)   // 标题文字
val Gray800 = Color(0xFF1F2937)   // 深色标题
val Gray900 = Color(0xFF111827)   // 最深文字

// ==================== 语义色系统 ====================
// 成功色 - 用于已完成、可用状态
val Success50 = Color(0xFFECFDF5)
val Success100 = Color(0xFFD1FAE5)
val Success500 = Color(0xFF10B981)
val Success600 = Color(0xFF059669)
val Success700 = Color(0xFF047857)

// 警告色 - 用于提醒、注意
val Warning50 = Color(0xFFFFFBEB)
val Warning100 = Color(0xFFFEF3C7)
val Warning500 = Color(0xFFF59E0B)
val Warning600 = Color(0xFFD97706)
val Warning700 = Color(0xFFB45309)

// 错误色 - 用于错误、危险操作
val Error50 = Color(0xFFFEF2F2)
val Error100 = Color(0xFFFEE2E2)
val Error500 = Color(0xFFEF4444)
val Error600 = Color(0xFFDC2626)
val Error700 = Color(0xFFB91C1C)

// 信息色 - 用于提示信息
val Info50 = Color(0xFFEFF6FF)
val Info100 = Color(0xFFDBEAFE)
val Info500 = Color(0xFF3B82F6)
val Info600 = Color(0xFF2563EB)

// ==================== 特殊色 ====================
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)

// 天气卡片渐变色 - 亮色模式
val WeatherSunnyStart = Color(0xFFFFF7ED)
val WeatherSunnyEnd = Color(0xFFFFEDD5)
val WeatherRainyStart = Color(0xFFEFF6FF)
val WeatherRainyEnd = Color(0xFFDBEAFE)
val WeatherColdStart = Color(0xFFF0F9FF)
val WeatherColdEnd = Color(0xFFE0F2FE)

// 天气卡片渐变色 - 深色模式
val WeatherSunnyStartDark = Color(0xFF3D2E1A)
val WeatherSunnyEndDark = Color(0xFF4A3520)
val WeatherRainyStartDark = Color(0xFF1A2744)
val WeatherRainyEndDark = Color(0xFF1E3A5F)

// 课程卡片颜色 - 亮色模式（用于区分不同课程）
val CourseColors = listOf(
    Color(0xFFEEF2FF),  // Indigo
    Color(0xFFFEF3C7),  // Amber
    Color(0xFFECFDF5),  // Emerald
    Color(0xFFFCE7F3),  // Pink
    Color(0xFFE0E7FF),  // Violet
    Color(0xFFCCFBF1),  // Teal
    Color(0xFFFEE2E2),  // Rose
    Color(0xFFDBEAFE),  // Blue
)

// 课程卡片颜色 - 深色模式
val CourseColorsDark = listOf(
    Color(0xFF2D3556),  // Indigo
    Color(0xFF3D3520),  // Amber
    Color(0xFF1D3A2E),  // Emerald
    Color(0xFF3D2535),  // Pink
    Color(0xFF2D3056),  // Violet
    Color(0xFF1D3A38),  // Teal
    Color(0xFF3D2525),  // Rose
    Color(0xFF1D2D4A),  // Blue
)

// ==================== 渐变色系统 ====================
// 主渐变 - 用于重要操作按钮
val PrimaryGradient = listOf(
    Color(0xFF6366F1),  // Primary
    Color(0xFF4F46E5),  // Primary Dark
)

// 次渐变 - 用于次要元素
val SecondaryGradient = listOf(
    Color(0xFF14B8A6),
    Color(0xFF0D9488),
)

// 成功渐变
val SuccessGradient = listOf(
    Color(0xFF10B981),
    Color(0xFF059669),
)

// 警告渐变
val WarningGradient = listOf(
    Color(0xFFF59E0B),
    Color(0xFFD97706),
)

// 错误渐变
val ErrorGradient = listOf(
    Color(0xFFEF4444),
    Color(0xFFDC2626),
)
