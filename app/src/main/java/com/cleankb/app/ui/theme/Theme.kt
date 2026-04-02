package com.cleankb.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ==================== 主题模式 ====================
enum class ThemeMode { SYSTEM, LIGHT, DARK }

// ==================== 浅色主题配色 ====================
private val LightColors = lightColorScheme(
    primary = Primary600,
    onPrimary = White,
    primaryContainer = Primary100,
    onPrimaryContainer = Primary800,
    secondary = Secondary600,
    onSecondary = White,
    secondaryContainer = Secondary100,
    onSecondaryContainer = Secondary700,
    tertiary = Warning600,
    onTertiary = White,
    tertiaryContainer = Warning50,
    onTertiaryContainer = Warning700,
    error = Error600,
    onError = White,
    errorContainer = Error50,
    onErrorContainer = Error700,
    background = Gray50,
    onBackground = Gray900,
    surface = White,
    onSurface = Gray900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray600,
    outline = Gray300,
    outlineVariant = Gray200,
    scrim = Black.copy(alpha = 0.5f),
    inverseSurface = Gray800,
    inverseOnSurface = Gray100,
    inversePrimary = Primary200,
    surfaceTint = Primary600,
)

// ==================== 深色主题配色 ====================
private val DarkColors = darkColorScheme(
    primary = Primary400,
    onPrimary = Primary900,
    primaryContainer = Primary700,
    onPrimaryContainer = Primary100,
    secondary = Secondary400,
    onSecondary = Secondary700,
    secondaryContainer = Secondary600,
    onSecondaryContainer = Secondary100,
    tertiary = Warning500,
    onTertiary = Warning700,
    tertiaryContainer = Warning600,
    onTertiaryContainer = Warning100,
    error = Error500,
    onError = Error700,
    errorContainer = Error600,
    onErrorContainer = Error100,
    background = Gray900,
    onBackground = Gray100,
    surface = Gray800,
    onSurface = Gray100,
    surfaceVariant = Gray700,
    onSurfaceVariant = Gray300,
    outline = Gray600,
    outlineVariant = Gray700,
    scrim = Black.copy(alpha = 0.6f),
    inverseSurface = Gray100,
    inverseOnSurface = Gray800,
    inversePrimary = Primary600,
    surfaceTint = Primary400,
)

// ==================== 天气卡片渐变色 ====================
data class WeatherGradient(
    val sunnyStart: Color,
    val sunnyEnd: Color,
    val rainyStart: Color,
    val rainyEnd: Color,
)

val LightWeatherGradient = WeatherGradient(
    sunnyStart = WeatherSunnyStart,
    sunnyEnd = WeatherSunnyEnd,
    rainyStart = WeatherRainyStart,
    rainyEnd = WeatherRainyEnd,
)

val DarkWeatherGradient = WeatherGradient(
    sunnyStart = WeatherSunnyStartDark,
    sunnyEnd = WeatherSunnyEndDark,
    rainyStart = WeatherRainyStartDark,
    rainyEnd = WeatherRainyEndDark,
)

@Composable
fun weatherGradient(isDark: Boolean = isSystemInDarkTheme()): WeatherGradient =
    if (isDark) DarkWeatherGradient else LightWeatherGradient

// ==================== 课程卡片颜色 ====================
@Composable
fun courseColors(isDark: Boolean = isSystemInDarkTheme()): List<Color> =
    if (isDark) CourseColorsDark else CourseColors

@Composable
fun CleanKbTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
