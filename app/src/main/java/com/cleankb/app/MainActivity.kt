package com.cleankb.app

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.cleankb.app.data.CampusService
import com.cleankb.app.ui.components.AppCard
import com.cleankb.app.ui.components.EmptyState
import com.cleankb.app.ui.components.HighlightCard
import com.cleankb.app.ui.components.LoadingState
import com.cleankb.app.ui.components.PrimaryButton
import com.cleankb.app.ui.components.SecondaryButton
import com.cleankb.app.ui.components.SectionHeader
import com.cleankb.app.ui.theme.CleanKbTheme
import com.cleankb.app.ui.theme.Radius
import com.cleankb.app.ui.theme.Spacing
import com.cleankb.app.ui.theme.ThemeMode
import com.cleankb.app.ui.theme.courseColors
import com.cleankb.app.ui.theme.weatherGradient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ==================== 枚举和常量定义 ====================
enum class QueryTab(val title: String) {
    TODAY("今日"),
    WEEK("本周"),
    FREE_ROOM("空教室")
}

private const val PREFS_NAME = "clean_kb_prefs"
private const val KEY_USER_ID = "saved_user_id"
private const val KEY_BUILDING_ORDER = "free_room_building_order"
private const val KEY_THEME_MODE = "theme_mode"
private const val DEFAULT_SERVICE_URL = "https://api.xiqueer.com/manager/"
private const val DEFAULT_USER_TYPE = "STU"
private const val DEFAULT_XXDM = "10467"
private const val DEFAULT_ROOM_LIMIT = 6
private val FREE_ROOM_BUILDING_OPTIONS = listOf(
    "弘善楼",
    "弘毅楼",
    "西校区(东)3号楼",
    "西校区(东)4号楼",
    "西校区(东)5号楼",
    "西校区(东)6号楼",
    "西校区(东)7号楼",
    "西校区(东)8号楼",
    "西校区(东)9号楼",
    "西校区(东)10号楼",
    "西校区(东)11号楼",
    "西校区(西)0号楼",
    "西校区(西)1号楼",
    "西校区(西)2号楼"
)

private sealed interface TabLoadResult {
    data class Today(val data: CampusService.TodaySchedule) : TabLoadResult
    data class Week(val data: CampusService.WeekSchedule) : TabLoadResult
    data class FreeRoom(val data: CampusService.FreeRoomSchedule) : TabLoadResult
}

private data class RequestUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val requestId: Int = 0,
)

private enum class AppPage {
    USER_SETUP,
    USER_CHANGE,
    MAIN,
    SETTINGS,
}

// ==================== 主 Activity ====================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CleanKbApp() }
    }
}

// ==================== 主应用组件 ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CleanKbApp() {
    val scope = rememberCoroutineScope()
    val service = remember { CampusService() }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var selectedTab by remember { mutableIntStateOf(0) }

    var userId by remember { mutableStateOf(prefs.getString(KEY_USER_ID, "")?.trim().orEmpty()) }
    var userIdInput by remember { mutableStateOf(userId) }
    var currentPage by remember { mutableStateOf(if (userId.isBlank()) AppPage.USER_SETUP else AppPage.MAIN) }

    var todayData by remember { mutableStateOf<CampusService.TodaySchedule?>(null) }
    var weekData by remember { mutableStateOf<CampusService.WeekSchedule?>(null) }
    var freeRoomData by remember { mutableStateOf<CampusService.FreeRoomSchedule?>(null) }
    var weatherData by remember { mutableStateOf<CampusService.WeatherSummary?>(null) }
    var selectedFreeRoomBuilding by remember { mutableStateOf<String?>(null) }
    var freeRoomBuildingOrder by remember { mutableStateOf(loadBuildingOrder(prefs)) }
    var todayUiState by remember { mutableStateOf(RequestUiState()) }
    var weekUiState by remember { mutableStateOf(RequestUiState()) }
    var freeRoomUiState by remember { mutableStateOf(RequestUiState()) }
    var weatherUiState by remember { mutableStateOf(RequestUiState()) }
    var activeTabJob by remember { mutableStateOf<Job?>(null) }
    var activeTabLoad by remember { mutableStateOf<QueryTab?>(null) }
    var weatherJob by remember { mutableStateOf<Job?>(null) }
    var refreshVersion by remember { mutableIntStateOf(0) }

    // 主题设置
    var themeMode by remember {
        mutableStateOf(
            ThemeMode.entries.getOrElse(
                prefs.getInt(KEY_THEME_MODE, 0)
            ) { ThemeMode.SYSTEM }
        )
    }
    val isDark = themeMode == ThemeMode.DARK ||
        (themeMode == ThemeMode.SYSTEM && androidx.compose.foundation.isSystemInDarkTheme())

    fun saveThemeMode(mode: ThemeMode) {
        themeMode = mode
        prefs.edit().putInt(KEY_THEME_MODE, mode.ordinal).apply()
    }

    // 更换学号弹窗
    var showUserIdDialog by remember { mutableStateOf(false) }
    var dialogUserIdInput by remember { mutableStateOf("") }

    // 页面背景渐变
    val background = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    )

    fun tabUiState(tab: QueryTab): RequestUiState {
        return when (tab) {
            QueryTab.TODAY -> todayUiState
            QueryTab.WEEK -> weekUiState
            QueryTab.FREE_ROOM -> freeRoomUiState
        }
    }

    fun updateTabUiState(tab: QueryTab, transform: (RequestUiState) -> RequestUiState) {
        when (tab) {
            QueryTab.TODAY -> todayUiState = transform(todayUiState)
            QueryTab.WEEK -> weekUiState = transform(weekUiState)
            QueryTab.FREE_ROOM -> freeRoomUiState = transform(freeRoomUiState)
        }
    }

    fun loadData(tab: QueryTab) {
        val id = userId.trim()
        if (id.isBlank()) {
            currentPage = AppPage.USER_SETUP
            return
        }

        val cfg = CampusService.CommonConfig(
            serviceUrl = DEFAULT_SERVICE_URL,
            userId = id,
            userType = DEFAULT_USER_TYPE,
            xxdm = DEFAULT_XXDM
        )

        activeTabJob?.cancel()
        val previousTab = activeTabLoad
        if (previousTab != null && previousTab != tab) {
            updateTabUiState(previousTab) {
                it.copy(
                    loading = false,
                    requestId = it.requestId + 1
                )
            }
        }
        activeTabLoad = tab
        val requestId = tabUiState(tab).requestId + 1
        updateTabUiState(tab) { it.copy(loading = true, error = null, requestId = requestId) }

        activeTabJob = scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    when (tab) {
                        QueryTab.TODAY -> TabLoadResult.Today(service.queryTodayData(cfg))
                        QueryTab.WEEK -> TabLoadResult.Week(service.queryWeekData(cfg))
                        QueryTab.FREE_ROOM -> TabLoadResult.FreeRoom(
                            service.queryFreeClassroomsData(
                                config = cfg,
                                limit = DEFAULT_ROOM_LIMIT,
                                currentPeriodOverride = null,
                                selectedBuilding = selectedFreeRoomBuilding
                            )
                        )
                    }
                }
            }

            if (tabUiState(tab).requestId != requestId) {
                return@launch
            }
            if (activeTabLoad == tab) {
                activeTabLoad = null
            }

            result.onSuccess { payload ->
                when (payload) {
                    is TabLoadResult.Today -> todayData = payload.data
                    is TabLoadResult.Week -> weekData = payload.data
                    is TabLoadResult.FreeRoom -> freeRoomData = payload.data
                }
            }
            updateTabUiState(tab) {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun loadWeather() {
        val id = userId.trim()
        if (id.isBlank()) {
            currentPage = AppPage.USER_SETUP
            return
        }

        weatherJob?.cancel()
        val requestId = weatherUiState.requestId + 1
        weatherUiState = weatherUiState.copy(loading = true, error = null, requestId = requestId)

        weatherJob = scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { service.queryWeatherData() } }
            if (weatherUiState.requestId != requestId) {
                return@launch
            }
            result.onSuccess { weatherData = it }
            weatherUiState = weatherUiState.copy(
                loading = false,
                error = if (result.isFailure) "天气加载失败" else null
            )
        }
    }

    LaunchedEffect(selectedTab, userId, refreshVersion) {
        if (userId.isNotBlank()) {
            val tab = QueryTab.entries[selectedTab]
            loadData(tab)
        }
    }

    LaunchedEffect(userId, refreshVersion) {
        if (userId.isNotBlank()) {
            loadWeather()
        }
    }

    fun submitUserId(newId: String) {
        val id = newId.trim()
        if (id.isBlank()) return
        userId = id
        prefs.edit().putString(KEY_USER_ID, id).apply()
        todayData = null
        weekData = null
        freeRoomData = null
        weatherData = null
        selectedFreeRoomBuilding = null
        activeTabJob?.cancel()
        weatherJob?.cancel()
        activeTabJob = null
        weatherJob = null
        activeTabLoad = null
        todayUiState = RequestUiState(requestId = todayUiState.requestId + 1)
        weekUiState = RequestUiState(requestId = weekUiState.requestId + 1)
        freeRoomUiState = RequestUiState(requestId = freeRoomUiState.requestId + 1)
        weatherUiState = RequestUiState(requestId = weatherUiState.requestId + 1)
        currentPage = AppPage.MAIN
        refreshVersion += 1
    }

    val currentTab = QueryTab.entries[selectedTab]
    val currentTabUiState = tabUiState(currentTab)

    // 页面路由
    when (currentPage) {
        AppPage.USER_SETUP -> {
            CleanKbTheme(themeMode = themeMode) {
                UserSetupPage(
                    value = userIdInput,
                    onValueChange = { userIdInput = it.trim() },
                    onSave = { submitUserId(userIdInput) }
                )
            }
            return
        }
        AppPage.USER_CHANGE -> {
            currentPage = AppPage.SETTINGS
            return
        }
        AppPage.SETTINGS -> {
            BackHandler(enabled = true) { currentPage = AppPage.MAIN }
            CleanKbTheme(themeMode = themeMode) {
                SettingsPage(
                    userId = userId,
                    themeMode = themeMode,
                    onThemeChange = { saveThemeMode(it) },
                    onChangeUserId = {
                        dialogUserIdInput = userId
                        showUserIdDialog = true
                    }
                )
                // 更换学号弹窗
                if (showUserIdDialog) {
                    AlertDialog(
                        onDismissRequest = { showUserIdDialog = false },
                        title = {
                            Text(
                                text = "更换学号",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                                Text(
                                    text = "当前学号：$userId",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = dialogUserIdInput,
                                    onValueChange = { dialogUserIdInput = it.trim() },
                                    label = { Text("新学号") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                                        cursorColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(Radius.sm)
                                )
                            }
                        },
                        confirmButton = {
                            PrimaryButton(
                                text = "确认更换",
                                onClick = {
                                    submitUserId(dialogUserIdInput)
                                    showUserIdDialog = false
                                },
                                enabled = dialogUserIdInput.trim().isNotBlank(),
                                modifier = Modifier.width(120.dp)
                            )
                        },
                        dismissButton = {
                            SecondaryButton(
                                text = "取消",
                                onClick = { showUserIdDialog = false },
                                modifier = Modifier.width(120.dp)
                            )
                        },
                        shape = RoundedCornerShape(Radius.lg)
                    )
                }
            }
            return
        }
        AppPage.MAIN -> {}
    }

    // 主页面
    Scaffold(
        bottomBar = {
            ModernNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .padding(innerPadding)
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Spacer(modifier = Modifier.height(Spacing.sm))

            // 天气卡片
            WeatherCard(
                data = weatherData,
                loading = weatherUiState.loading,
                error = weatherUiState.error,
                isDark = isDark,
                onOpenSettings = { currentPage = AppPage.SETTINGS }
            )

            // 错误提示
            if (currentTabUiState.error != null) {
                AppCard(
                    gradientColors = listOf(
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = currentTabUiState.error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // 内容区域
            Box(modifier = Modifier.fillMaxSize()) {
                if (currentTabUiState.loading) {
                    LoadingState(message = "正在加载...")
                } else {
                    when (currentTab) {
                        QueryTab.TODAY -> TodayTab(todayData, isDark)
                        QueryTab.WEEK -> WeekTab(weekData, isDark)
                        QueryTab.FREE_ROOM -> FreeRoomTab(
                            data = freeRoomData,
                            selectedBuilding = selectedFreeRoomBuilding,
                            buildingOrder = freeRoomBuildingOrder,
                            onBuildingOrderChange = { next ->
                                freeRoomBuildingOrder = normalizeBuildingOrder(next)
                                prefs.edit()
                                    .putString(KEY_BUILDING_ORDER, freeRoomBuildingOrder.joinToString("|"))
                                    .apply()
                            },
                            onBuildingSelect = { b ->
                                selectedFreeRoomBuilding = b
                                loadData(QueryTab.FREE_ROOM)
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==================== 现代导航栏 ====================
@Composable
private fun ModernNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        QueryTab.entries.forEachIndexed { index, tab ->
            val selected = selectedTab == index
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(index) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                ),
                icon = {
                    Icon(
                        imageVector = when (tab) {
                            QueryTab.TODAY -> Icons.Filled.Today
                            QueryTab.WEEK -> Icons.Filled.CalendarMonth
                            QueryTab.FREE_ROOM -> Icons.Filled.MeetingRoom
                        },
                        contentDescription = tab.title
                    )
                },
                label = {
                    Text(
                        tab.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

// ==================== 天气卡片 ====================
@Composable
private fun WeatherCard(
    data: CampusService.WeatherSummary?,
    loading: Boolean,
    error: String?,
    isDark: Boolean,
    onOpenSettings: () -> Unit
) {
    val wg = weatherGradient(isDark)

    // 根据天气状态选择渐变色
    val gradientColors = when {
        data?.rainToday == true -> listOf(wg.rainyStart, wg.rainyEnd)
        data?.maxTempC != null && data.maxTempC >= 30 -> listOf(wg.sunnyStart, wg.sunnyEnd)
        else -> listOf(wg.sunnyStart, wg.sunnyEnd)
    }

    AppCard(gradientColors = gradientColors) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // 顶部：位置 + 设置
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data?.title ?: "校园天气",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "设置",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 核心内容
            when {
                loading && data == null -> {
                    Text(
                        text = "正在获取天气...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                data != null -> {
                    // 温度行：当前温度 + 体感 | 温度范围
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            Text(
                                text = "${fmtTemp(data.currentTempC)}°",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${data.currentWeatherText} 体感${fmtTemp(data.feelTempC)}°",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${fmtTemp(data.minTempC)}°~${fmtTemp(data.maxTempC)}°",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 提醒文案：只显示一条关键提示
                    val tip = data.extraTips.firstOrNull() ?: data.reminder
                    if (tip.isNotBlank()) {
                        Text(
                            text = tip,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                else -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudQueue,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "天气暂无数据",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 错误信息
            if (error != null && data == null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ==================== 设置页面 ====================
@Composable
private fun SettingsPage(
    userId: String,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onChangeUserId: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            )
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // 学号设置
        AppCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "学号",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = userId,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                SecondaryButton(
                    text = "更换",
                    onClick = onChangeUserId,
                    modifier = Modifier.width(80.dp)
                )
            }
        }

        // 主题设置
        AppCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    text = "主题模式",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val selected = themeMode == mode
                        val label = when (mode) {
                            ThemeMode.SYSTEM -> "跟随系统"
                            ThemeMode.LIGHT -> "浅色模式"
                            ThemeMode.DARK -> "深色模式"
                        }
                        FilterChip(
                            selected = selected,
                            onClick = { onThemeChange(mode) },
                            label = {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = MaterialTheme.colorScheme.outline,
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                enabled = true,
                                selected = selected
                            )
                        )
                    }
                }
            }
        }
    }
}

// ==================== 用户设置页面 ====================
@Composable
private fun UserSetupPage(
    value: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    )
                )
            )
            .padding(Spacing.xl),
        contentAlignment = Alignment.Center
    ) {
        AppCard(
            gradientColors = listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Text(
                        text = "欢迎使用课表",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "首次使用请先输入学号，保存后会自动记住",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = { Text("学号") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(Radius.sm)
                )

                PrimaryButton(
                    text = "保存并进入",
                    onClick = onSave,
                    enabled = value.trim().isNotBlank()
                )
            }
        }
    }
}

// ==================== 更换学号页面 ====================
@Composable
private fun ChangeUserIdPage(
    currentUserId: String,
    value: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            )
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        Text(
            text = "更换学号",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "当前学号：$currentUserId",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        AppCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = { Text("新学号") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(Radius.sm)
                )

                PrimaryButton(
                    text = "确认更换并刷新",
                    onClick = onSave,
                    enabled = value.trim().isNotBlank()
                )
            }
        }
    }
}

// ==================== 今日课表 ====================
@Composable
private fun TodayTab(data: CampusService.TodaySchedule?, isDark: Boolean) {
    if (data == null) {
        return EmptyState(
            icon = Icons.Filled.Today,
            title = "暂无课表数据",
            subtitle = "请检查网络连接或学号设置"
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        contentPadding = PaddingValues(bottom = Spacing.lg)
    ) {
        item {
            SectionHeader(
                title = "今天课程",
                subtitle = "第${data.currentWeek}周 | ${weekdayName(data.weekday)}"
            )
        }

        if (data.courses.isEmpty()) {
            item {
                AppCard {
                    EmptyState(
                        icon = Icons.Filled.Weekend,
                        title = "今天没课",
                        subtitle = "好好休息，享受自由时光"
                    )
                }
            }
        } else {
            items(
                items = data.courses,
                key = { "${it.name}-${it.section}-${it.location}-${it.beginTime}" }
            ) { course ->
                CourseCard(
                    course = course,
                    index = data.courses.indexOf(course),
                    isDark = isDark
                )
            }
        }
    }
}

// ==================== 本周课表 ====================
@Composable
private fun WeekTab(data: CampusService.WeekSchedule?, isDark: Boolean) {
    if (data == null) {
        return EmptyState(
            icon = Icons.Filled.CalendarMonth,
            title = "暂无课表数据",
            subtitle = "请检查网络连接或学号设置"
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        contentPadding = PaddingValues(bottom = Spacing.lg)
    ) {
        item {
            SectionHeader(
                title = "本周课表",
                subtitle = "第${data.currentWeek}周"
            )
        }

        items(items = data.days, key = { it.weekday }) { day ->
            WeekDayCard(day = day)
        }
    }
}

@Composable
private fun WeekDayCard(day: CampusService.WeekDaySchedule) {
    AppCard {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = weekdayName(day.weekday),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${day.courses.size} 门课",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (day.courses.isEmpty()) {
                Text(
                    text = "无课程安排",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                day.courses.forEachIndexed { index, course ->
                    CourseItemCompact(course = course)
                    if (index < day.courses.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = Spacing.xs),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

// ==================== 课程卡片 ====================
@Composable
private fun CourseCard(course: CampusService.CourseItem, index: Int, isDark: Boolean) {
    val colors = courseColors(isDark)
    val cardColor = colors[index % colors.size]

    AppCard(
        gradientColors = listOf(
            cardColor,
            cardColor.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            // 课程名称
            Text(
                text = course.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 节次和时间
            val timeText = formatCourseTime(course.beginTime, course.endTime)
            val sectionText = compactSectionText(course.section)
            if (sectionText.isNotBlank() || timeText.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    if (sectionText.isNotBlank()) {
                        Text(
                            text = "第${sectionText}节",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

            // 教室和老师
            val locationText = displayLocation(course.location)
            if (locationText.isNotBlank() || course.teacher.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    if (locationText.isNotBlank()) {
                        Text(
                            text = locationText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (course.teacher.isNotBlank()) {
                        Text(
                            text = course.teacher,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseItemCompact(course: CampusService.CourseItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = course.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        val details = buildString {
            val section = compactSectionText(course.section)
            if (section.isNotBlank()) append("第${section}节")
            val location = displayLocation(course.location)
            if (location.isNotBlank()) {
                if (isNotBlank()) append(" · ")
                append(location)
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

// ==================== 空教室页面 ====================
@Composable
private fun FreeRoomTab(
    data: CampusService.FreeRoomSchedule?,
    selectedBuilding: String?,
    buildingOrder: List<String>,
    onBuildingOrderChange: (List<String>) -> Unit,
    onBuildingSelect: (String?) -> Unit
) {
    if (data == null) {
        return EmptyState(
            icon = Icons.Filled.MeetingRoom,
            title = "暂无空教室数据",
            subtitle = "请检查网络连接或学号设置"
        )
    }

    val density = LocalDensity.current
    val chipRowState = rememberLazyListState()
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragPointerViewportX by remember { mutableStateOf(0f) }
    var dragTouchAnchorX by remember { mutableStateOf(0f) }
    var autoScrollSpeed by remember { mutableStateOf(0f) }
    val anchorOption = mappedOptionForAnchor(data.anchorBuilding)
    val orderedOptions = buildingOrder
    val edgeTriggerPx = with(density) { 88.dp.toPx() }
    val maxAutoScrollPerFrame = with(density) { 4.dp.toPx() }

    fun moveItem(key: String, targetKey: String): Boolean {
        if (key == targetKey) return false
        val from = buildingOrder.indexOf(key)
        val to = buildingOrder.indexOf(targetKey)
        if (from < 0 || to < 0 || from == to) return false
        val ordered = buildingOrder.toMutableList()
        val moved = ordered.removeAt(from)
        ordered.add(to, moved)
        onBuildingOrderChange(ordered)
        return true
    }

    fun updateAutoScrollSpeed(key: String) {
        val info = chipRowState.layoutInfo
        if (info.visibleItemsInfo.none { it.key == key }) {
            autoScrollSpeed = 0f
            return
        }
        val leftOverflow = edgeTriggerPx - (dragPointerViewportX - info.viewportStartOffset)
        val rightOverflow = edgeTriggerPx - (info.viewportEndOffset - dragPointerViewportX)
        autoScrollSpeed = when {
            leftOverflow > 0f -> -maxAutoScrollPerFrame * (leftOverflow / edgeTriggerPx).coerceIn(0f, 1f)
            rightOverflow > 0f -> maxAutoScrollPerFrame * (rightOverflow / edgeTriggerPx).coerceIn(0f, 1f)
            else -> 0f
        }
    }

    fun maybeReorder(key: String) {
        val info = chipRowState.layoutInfo
        val dragged = info.visibleItemsInfo.firstOrNull { it.key == key } ?: return
        val draggedIndex = buildingOrder.indexOf(key)
        if (draggedIndex < 0) return
        val draggedStart = dragPointerViewportX - dragTouchAnchorX
        val draggedCenter = draggedStart + dragged.size / 2f
        val target = info.visibleItemsInfo
            .mapNotNull { item ->
                val itemKey = item.key as? String ?: return@mapNotNull null
                if (itemKey == key) return@mapNotNull null
                val itemIndex = buildingOrder.indexOf(itemKey)
                if (itemIndex < 0) return@mapNotNull null
                val center = item.offset + item.size / 2f
                val crossed = if (itemIndex > draggedIndex) {
                    draggedCenter > center
                } else {
                    draggedCenter < center
                }
                if (!crossed) return@mapNotNull null
                Triple(item, itemKey, kotlin.math.abs(draggedCenter - center))
            }
            .minByOrNull { it.third }
        if (target != null) {
            moveItem(key, target.second)
        }
    }

    fun handleDragDelta(key: String, deltaX: Float) {
        if (draggingKey != key) return
        dragPointerViewportX += deltaX
        maybeReorder(key)
        updateAutoScrollSpeed(key)
    }

    LaunchedEffect(draggingKey) {
        while (isActive && draggingKey != null) {
            withFrameNanos { }
            val key = draggingKey ?: break
            val speed = autoScrollSpeed
            if (speed == 0f) continue
            val consumed = chipRowState.scrollBy(speed)
            if (consumed == 0f) {
                autoScrollSpeed = 0f
                continue
            }
            maybeReorder(key)
            updateAutoScrollSpeed(key)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        contentPadding = PaddingValues(bottom = Spacing.lg)
    ) {
        // 楼栋选择器
        item {
            val anchorLabel = data.anchorBuilding.ifBlank { "未定位最近楼" }
            LazyRow(
                state = chipRowState,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                userScrollEnabled = draggingKey == null
            ) {
                item {
                    if (anchorOption == null || anchorOption !in orderedOptions) {
                        BuildingChip(
                            label = anchorLabel,
                            selected = selectedBuilding == null,
                            onClick = { onBuildingSelect(null) }
                        )
                    }
                }
                items(orderedOptions, key = { it }) { building ->
                    val isAnchorChip = building == anchorOption
                    val itemOffset = chipRowState.layoutInfo.visibleItemsInfo
                        .firstOrNull { it.key == building }
                        ?.offset
                        ?.toFloat()
                        ?: 0f
                    ReorderableBuildingChip(
                        label = if (isAnchorChip) anchorLabel else building,
                        selected = if (isAnchorChip) {
                            selectedBuilding == null || selectedBuilding == anchorOption
                        } else {
                            selectedBuilding == building
                        },
                        onClick = {
                            if (isAnchorChip) onBuildingSelect(null) else onBuildingSelect(building)
                        },
                        isDragging = draggingKey == building,
                        dragOffsetX = if (draggingKey == building) {
                            dragPointerViewportX - dragTouchAnchorX - itemOffset
                        } else {
                            0f
                        },
                        onDragStart = { touchX ->
                            draggingKey = building
                            dragTouchAnchorX = touchX
                            dragPointerViewportX = itemOffset + touchX
                            autoScrollSpeed = 0f
                            updateAutoScrollSpeed(building)
                        },
                        onDragDelta = { deltaX -> handleDragDelta(building, deltaX) },
                        onDragStop = {
                            draggingKey = null
                            dragPointerViewportX = 0f
                            dragTouchAnchorX = 0f
                            autoScrollSpeed = 0f
                        }
                    )
                }
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
                    EmptyState(
                        icon = Icons.Filled.SearchOff,
                        title = msg
                    )
                }
            } else {
                HighlightCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "推荐",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = r.room,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            val compactBuilding = compactFreeRoomBuilding(r.room, r.building)
                            if (compactBuilding.isNotBlank()) {
                                Text(
                                    text = compactBuilding,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        FreePeriodsBlock(periods = r.freePeriods)
                    }
                }
            }
        }

        // 其他空教室列表
        if (data.rooms.isNotEmpty()) {
            val compactRooms = data.rooms.take(DEFAULT_ROOM_LIMIT)
            item {
                AppCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        compactRooms.forEachIndexed { idx, room ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = room.room,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val compactBuilding = compactFreeRoomBuilding(room.room, room.building)
                                    if (compactBuilding.isNotBlank()) {
                                        Text(
                                            text = compactBuilding,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                FreePeriodsBlock(periods = room.freePeriods)
                            }
                            if (idx != compactRooms.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BuildingChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.primary,
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
            enabled = true,
            selected = selected
        )
    )
}

@Composable
private fun ReorderableBuildingChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    isDragging: Boolean,
    dragOffsetX: Float,
    onDragStart: (Float) -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragStop: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val startState = rememberUpdatedState(onDragStart)
    val deltaState = rememberUpdatedState(onDragDelta)
    val stopState = rememberUpdatedState(onDragStop)
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "buildingChipScale"
    )
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "buildingChipElevation"
    )

    Box(
        modifier = Modifier
            .zIndex(if (isDragging) 2f else 0f)
            .shadow(elevation, RoundedCornerShape(Radius.full))
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.translationX = if (isDragging) dragOffsetX else 0f
            }
            .pointerInput(label) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        startState.value(offset.x)
                    },
                    onDragEnd = { stopState.value() },
                    onDragCancel = { stopState.value() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        deltaState.value(dragAmount.x)
                    }
                )
            }
    ) {
        BuildingChip(
            label = label,
            selected = selected,
            onClick = onClick
        )
    }
}

@Composable
private fun FreePeriodsBlock(periods: List<Int>) {
    Column(
        modifier = Modifier.width(100.dp),
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

// ==================== 工具函数 ====================
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
    val first = Regex("\\d+").find(s)?.value
    return first.orEmpty()
}

private fun formatPeriods(periods: List<Int>): String {
    if (periods.isEmpty()) return "-"
    return periods.joinToString(" ")
}

private fun fmtTemp(v: Double): String = kotlin.math.round(v).toInt().toString()

private fun fmtNum(v: Double): String = String.format("%.1f", v)

private fun displayLocation(raw: String): String {
    val s = raw.trim()
    val m = Regex("\\[(.+)]").find(s)
    if (m != null) return m.groupValues[1].trim()
    return s
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

private fun normalizeBuildingOrder(order: List<String>): List<String> {
    val filtered = order.distinct().filter { it in FREE_ROOM_BUILDING_OPTIONS }
    return filtered + FREE_ROOM_BUILDING_OPTIONS.filterNot { it in filtered }
}

private fun loadBuildingOrder(prefs: android.content.SharedPreferences): List<String> {
    val raw = prefs.getString(KEY_BUILDING_ORDER, "")?.trim().orEmpty()
    if (raw.isBlank()) return FREE_ROOM_BUILDING_OPTIONS
    return normalizeBuildingOrder(raw.split('|').map { it.trim() }.filter { it.isNotBlank() })
}

private fun mappedOptionForAnchor(anchorBuilding: String): String? {
    return when (anchorBuilding.trim()) {
        "弘善楼" -> "弘善楼"
        "弘毅楼" -> "弘毅楼"
        "3号楼" -> "西校区(东)3号楼"
        "4号楼" -> "西校区(东)4号楼"
        "5号楼" -> "西校区(东)5号楼"
        "6号楼" -> "西校区(东)6号楼"
        "7号楼" -> "西校区(东)7号楼"
        "8号楼" -> "西校区(东)8号楼"
        "9号楼" -> "西校区(东)9号楼"
        "10号楼" -> "西校区(东)10号楼"
        "11号楼" -> "西校区(东)11号楼"
        "0号楼" -> "西校区(西)0号楼"
        "1号楼" -> "西校区(西)1号楼"
        "2号楼" -> "西校区(西)2号楼"
        else -> null
    }
}
