package com.cleankb.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

class CampusService {

    data class CommonConfig(
        val serviceUrl: String,
        val userId: String,
        val userType: String,
        val xxdm: String,
        val encryptKey: String = "yt6n78",
        val encryptSecretKey: String = "ab",
        val appInfo: String = "android2.6.449",
    )

    private data class RoomCandidate(
        val room: String,
        val building: String,
        val capacity: Int,
        val roomType: String,
        val freePeriods: List<Int>,
        val rawStatus: List<String>,
    )

    private data class BuildingScanResult(
        val rooms: List<RoomCandidate>,
        val periodCount: Map<Int, Int>,
    )

    private data class DetailCacheEntry(
        val at: Long,
        val detail: JSONObject?,
    )

    data class CourseItem(
        val name: String,
        val section: String,
        val location: String,
        val teacher: String,
        val beginTime: String,
        val endTime: String,
    )

    data class TodaySchedule(
        val date: LocalDate,
        val weekday: Int,
        val term: String,
        val currentWeek: String,
        val courses: List<CourseItem>,
    )

    data class WeekDaySchedule(
        val weekday: Int,
        val courses: List<CourseItem>,
    )

    data class WeekSchedule(
        val date: LocalDate,
        val term: String,
        val currentWeek: String,
        val days: List<WeekDaySchedule>,
    )

    data class WeatherSummary(
        val title: String,
        val date: LocalDate,
        val rainToday: Boolean,
        val rainStart: String?,
        val rainEnd: String?,
        val rainPeriodText: String?,
        val minTempC: Double,
        val maxTempC: Double,
        val currentTempC: Double,
        val sunrise: String,
        val sunset: String,
        val windSpeedMps: Double,
        val reminder: String,
    )

    data class FreeRoomItem(
        val room: String,
        val building: String,
        val capacity: Int,
        val startPeriod: Int,
        val runLength: Int,
        val freePeriods: List<Int>,
    )

    data class FreeRoomSchedule(
        val date: LocalDate,
        val weekday: Int,
        val term: String,
        val currentWeek: String,
        val season: String,
        val estimatedPeriod: Int,
        val currentPeriod: Int?,
        val nextPeriod: Int?,
        val timeStatus: String,
        val targetPeriod: Int,
        val anchorBuilding: String,
        val activeBuilding: String?,
        val availableBuildings: List<String>,
        val busyPeriods: List<Int>,
        val freePeriods: List<Int>,
        val recommended: FreeRoomItem?,
        val rooms: List<FreeRoomItem>,
    )

    private data class PeriodContext(
        val currentPeriod: Int?,
        val nextPeriod: Int?,
        val status: String,
    )

    private val cacheLock = Any()
    private val detailCacheTtlMs = 5 * 60 * 1000L
    private var jxlListCacheAt: Long = 0L
    private var jxlListCache: List<Pair<String, String>> = emptyList()
    private val detailCache = mutableMapOf<String, DetailCacheEntry>()
    private val weatherHeaders = linkedMapOf(
        "accept" to "application/json",
        "accept-language" to "zh-CN,zh;q=0.9,en;q=0.8,zh-HK;q=0.7",
        "cache-control" to "no-cache",
        "origin" to "https://www.caiyunapp.com",
        "pragma" to "no-cache",
        "priority" to "u=1, i",
        "referer" to "https://www.caiyunapp.com/",
        "sec-ch-ua" to "\"Not:A-Brand\";v=\"99\", \"Google Chrome\";v=\"145\", \"Chromium\";v=\"145\"",
        "sec-ch-ua-mobile" to "?0",
        "sec-ch-ua-platform" to "\"Windows\"",
        "sec-fetch-dest" to "empty",
        "sec-fetch-mode" to "cors",
        "sec-fetch-site" to "same-site",
        "user-agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36",
    )

    private val caiyunTargetApi =
        "https://api.caiyunapp.com/v2/<t1>/113.94,35.28/forecast?dailysteps=1&alert=true"
    private val rainThresholdMm = 0.0
    private val rainPeriodOrder = listOf("凌晨", "早上", "中午", "下午", "晚上")

    private fun normalizeApiUrl(serviceUrl: String): String {
        return serviceUrl.trim().removeSuffix("/") + "/wap/wapController.jsp"
    }

    private fun normalizeUserId(userId: String, xxdm: String): String {
        val id = userId.trim()
        return if ("_" in id || xxdm.isBlank()) id else "${xxdm.trim()}_$id"
    }

    private fun md5Hex(s: String): String {
        val md = MessageDigest.getInstance("MD5")
        val b = md.digest(s.toByteArray(StandardCharsets.UTF_8))
        return b.joinToString("") { "%02x".format(it) }
    }

    private fun toBase36(value: Long): String {
        if (value == 0L) return "0"
        var n = value
        val chars = "0123456789abcdefghijklmnopqrstuvwxyz"
        val sb = StringBuilder()
        while (n > 0) {
            val r = (n % 36).toInt()
            sb.append(chars[r])
            n /= 36
        }
        return sb.reverse().toString()
    }

    private fun encryptParam(raw: String, key: String): String {
        if (raw.isBlank() || key.isBlank()) return raw
        val keyLen = key.length
        val rawLen = raw.length
        val rounds = ceil(rawLen / keyLen.toDouble()).toInt()
        val plus = (ceil((((rawLen * 3.0) * 6.0) / 9.0) / 6.0).toInt() * 6) % keyLen

        val s3 = StringBuilder()
        for (i in 0 until rounds) {
            for (j in 1..keyLen) {
                val idx = i * keyLen + j
                if (idx > rawLen) break
                val v = raw[idx - 1].code + key[j - 1].code + plus
                s3.append(("000$v").takeLast(3))
            }
        }

        val out = StringBuilder()
        var i = 0
        while (i < s3.length) {
            val end = minOf(i + 9, s3.length)
            val chunk = s3.substring(i, end)
            out.append(("000000" + toBase36(chunk.toLong())).takeLast(6))
            i += 9
        }
        return out.toString()
    }

    private fun calcParam2(raw: String): String {
        val d = md5Hex(raw)
        val arr = listOf("") + d.map { it.toString() }
        val s = buildString {
            for (i in arr.indices) {
                if (i != 3 && i != 10 && i != 17 && i != 25) {
                    append(arr[i])
                }
            }
        }
        return md5Hex(s)
    }

    private fun joinRawPairs(pairs: LinkedHashMap<String, String>): String {
        return pairs.entries.joinToString("&") { "${it.key}=${it.value}" }
    }

    private fun postEncryptedJson(apiUrl: String, pairs: LinkedHashMap<String, String>, cfg: CommonConfig): JSONObject? {
        val raw = joinRawPairs(pairs)
        val payload = linkedMapOf(
            "param" to encryptParam(raw, cfg.encryptKey),
            "param2" to calcParam2(raw),
            "encrptSecretKey" to cfg.encryptSecretKey,
            "appinfo" to cfg.appInfo,
            "timestamp" to (System.currentTimeMillis() / 1000L).toString(),
            "echo" to randomEcho(),
            "xqerSign" to md5Hex("x"),
        )
        return postForm(apiUrl, payload)
    }

    private fun postForm(url: String, data: LinkedHashMap<String, String>): JSONObject? {
        val body = data.entries.joinToString("&") {
            URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8")
        }
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 20000
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            setRequestProperty("Accept", "application/json,text/plain,*/*")
            setRequestProperty("User-Agent", "CleanKB/1.0")
        }
        return try {
            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(body) }
            val code = conn.responseCode
            val input = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = BufferedReader(input.reader(StandardCharsets.UTF_8)).use { r ->
                r.readLines().joinToString("\n")
            }
            if (text.isBlank()) null else JSONObject(text)
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun currentXnxq(apiUrl: String, userId: String, cfg: CommonConfig): String {
        val base = linkedMapOf(
            "userId" to userId,
            "usertype" to cfg.userType,
        )
        if (cfg.xxdm.isNotBlank()) base["xxdm"] = cfg.xxdm.trim()

        for ((action, step) in listOf("getXtgn" to "xnxq", "getKb" to "xnxq")) {
            val p = LinkedHashMap(base)
            p["action"] = action
            p["step"] = step
            val j = postEncryptedJson(apiUrl, p, cfg) ?: continue
            val arr = j.optJSONArray("xnxq") ?: continue
            if (arr.length() == 0) continue
            for (i in 0 until arr.length()) {
                val t = arr.optJSONObject(i) ?: continue
                if (t.optString("dqxq").trim() == "1") {
                    val dm = t.optString("dm").trim()
                    if (dm.isNotBlank()) return dm
                }
            }
            val dm = arr.optJSONObject(0)?.optString("dm", "")?.trim().orEmpty()
            if (dm.isNotBlank()) return dm
        }
        error("无法获取学期 xnxq")
    }

    private fun fetchKbDetail(apiUrl: String, userId: String, xnxq: String, week: String, cfg: CommonConfig): JSONObject {
        val common = linkedMapOf(
            "action" to "getKb",
            "step" to "kbdetail_bz",
            "bjdm" to "",
            "jsdm" to "",
            "xnxq" to xnxq,
            "week" to week,
            "usertype" to cfg.userType,
        )
        if (cfg.xxdm.isNotBlank()) common["xxdm"] = cfg.xxdm.trim()

        val suffix = userId.substringAfter("_", userId)
        val branches = listOf(
            linkedMapOf<String, String>().apply {
                putAll(common); put("userId", userId); put("channel", "jrkb")
            },
            linkedMapOf<String, String>().apply {
                putAll(common); put("userid", suffix)
            },
            linkedMapOf<String, String>().apply {
                putAll(common); put("userId", suffix); put("uuid", userId)
            }
        )
        for (p in branches) {
            val j = postEncryptedJson(apiUrl, p, cfg)
            if (j != null && j.has("zc")) return j
        }
        error("无法获取课表详情")
    }

    private fun periodSortKey(c: JSONObject): Int {
        val jcxx = c.optString("jcxx", "")
        val m = Regex("\\d+").find(jcxx)
        return m?.value?.toIntOrNull() ?: 999
    }

    private fun mapCourse(c: JSONObject): CourseItem {
        return CourseItem(
            name = c.optString("kcmc", "(无课程名)").trim(),
            section = c.optString("jcxx", "").trim(),
            location = c.optString("skdd", "").trim(),
            teacher = c.optString("rkjs", "").trim().ifBlank { c.optString("jsdm", "").trim() },
            beginTime = c.optString("beginTime", "").trim(),
            endTime = c.optString("endTime", "").trim(),
        )
    }

    private fun formatCourse(c: CourseItem): String {
        val name = c.name
        val sec = c.section
        val loc = c.location
        val teacher = c.teacher
        val begin = c.beginTime
        val end = c.endTime

        val parts = mutableListOf(name)
        if (sec.isNotBlank()) parts += "[$sec]"
        if (begin.isNotBlank() || end.isNotBlank()) parts += "$begin-$end".trim('-')
        if (loc.isNotBlank()) parts += "@$loc"
        if (teacher.isNotBlank()) parts += "任课:$teacher"
        return parts.joinToString(" ")
    }

    fun queryTodayData(config: CommonConfig): TodaySchedule {
        val userId = normalizeUserId(config.userId, config.xxdm)
        val api = normalizeApiUrl(config.serviceUrl)
        val xnxq = currentXnxq(api, userId, config)
        val kb = fetchKbDetail(api, userId, xnxq, "", config)
        val weekday = LocalDateTime.now().dayOfWeek.value
        val arr = kb.optJSONArray("week$weekday") ?: JSONArray()

        val list = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
            .sortedBy { periodSortKey(it) }
            .map { mapCourse(it) }

        return TodaySchedule(
            date = LocalDate.now(),
            weekday = weekday,
            term = xnxq,
            currentWeek = kb.optString("zc"),
            courses = list
        )
    }

    fun queryToday(config: CommonConfig): String {
        val data = queryTodayData(config)
        return buildString {
            appendLine("日期: ${data.date}  周${data.weekday}")
            appendLine("学期: ${data.term}  当前周(接口): ${data.currentWeek}")
            appendLine("--------------------------------------------------")
            if (data.courses.isEmpty()) {
                appendLine("今天没有课程。")
            } else {
                data.courses.forEachIndexed { i, c -> appendLine("${i + 1}. ${formatCourse(c)}") }
            }
        }
    }

    fun queryWeekData(config: CommonConfig): WeekSchedule {
        val userId = normalizeUserId(config.userId, config.xxdm)
        val api = normalizeApiUrl(config.serviceUrl)
        val xnxq = currentXnxq(api, userId, config)
        val kb = fetchKbDetail(api, userId, xnxq, "", config)

        val days = (1..7).map { i ->
            val arr = kb.optJSONArray("week$i") ?: JSONArray()
            val list = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
                .sortedBy { periodSortKey(it) }
                .map { mapCourse(it) }
            WeekDaySchedule(weekday = i, courses = list)
        }
        return WeekSchedule(
            date = LocalDate.now(),
            term = xnxq,
            currentWeek = kb.optString("zc"),
            days = days
        )
    }

    fun queryWeek(config: CommonConfig): String {
        val data = queryWeekData(config)
        val dayName = mapOf(1 to "周一", 2 to "周二", 3 to "周三", 4 to "周四", 5 to "周五", 6 to "周六", 7 to "周日")
        return buildString {
            appendLine("日期: ${data.date}")
            appendLine("学期: ${data.term}  当前周(接口): ${data.currentWeek}")
            appendLine("============================================================")
            for (d in data.days) {
                appendLine("${dayName[d.weekday]}（${d.courses.size} 门）")
                if (d.courses.isEmpty()) {
                    appendLine("  - 无课程")
                } else {
                    d.courses.forEachIndexed { idx, c -> appendLine("  ${idx + 1}. ${formatCourse(c)}") }
                }
                if (d.weekday != 7) appendLine("------------------------------------------------------------")
            }
        }
    }

    fun queryWeatherData(): WeatherSummary {
        var lastError: Throwable? = null
        repeat(2) {
            try {
                return queryWeatherByCaiyun(caiyunTargetApi)
            } catch (t: Throwable) {
                lastError = t
            }
        }
        throw lastError ?: IllegalStateException("天气加载失败")
    }

    private fun queryWeatherByCaiyun(targetApi: String): WeatherSummary {
        val ticketBody = postText(
            url = "https://h5.caiyunapp.com/api/ticket",
            headers = LinkedHashMap(weatherHeaders).apply { put("content-length", "0") },
            body = "",
            timeoutMs = 10_000
        )
        val ticket = JSONObject(ticketBody).optString("ticket").trim()
        if (ticket.isBlank()) error("天气 ticket 获取失败")

        val payload = JSONObject().put("url", targetApi).toString()
        val weatherBody = postText(
            url = "https://h5.caiyunapp.com/api/?ticket=${URLEncoder.encode(ticket, "UTF-8")}",
            headers = LinkedHashMap(weatherHeaders).apply { put("content-type", "application/json") },
            body = payload,
            timeoutMs = 12_000
        )
        val obj = JSONObject(weatherBody)
        return if (obj.has("rain_today")) {
            extractProvidedSummary(obj)
        } else {
            extractCaiyunSummary(obj)
        }
    }

    private fun extractProvidedSummary(j: JSONObject): WeatherSummary {
        if (!j.has("rain_today")) {
            error("天气接口未返回 rain_today，请确认目标接口返回的是你提供的汇总格式")
        }

        val date = LocalDate.parse(j.optString("date", LocalDate.now().toString()))
        val rainToday = j.optBoolean("rain_today", false)
        val rainStart = j.optJSONObject("rain_start_time")
            ?.opt("value")
            ?.toString()
            ?.takeIf { it.isNotBlank() && it != "null" }
        val rainEnd = j.optJSONObject("rain_end_time")
            ?.opt("value")
            ?.toString()
            ?.takeIf { it.isNotBlank() && it != "null" }

        val todayTemp = j.optJSONObject("today_temperature")
        val minTemp = todayTemp?.optJSONObject("min")?.optDouble("value", 0.0) ?: 0.0
        val maxTemp = todayTemp?.optJSONObject("max")?.optDouble("value", 0.0) ?: 0.0
        val currentTemp = j.optJSONObject("current_temperature")?.optDouble("value", 0.0) ?: 0.0
        val windObj = j.optJSONObject("current_wind_speed")
        val windRaw = windObj?.optDouble("value", 0.0) ?: 0.0
        val windUnit = windObj?.optString("unit", "m/s").orEmpty().lowercase()
        val wind = if (windUnit == "km/h") kmhToMps(windRaw) else windRaw
        val sunrise = j.optJSONObject("sunrise")?.optString("value", "--:--").orEmpty()
        val sunset = j.optJSONObject("sunset")?.optString("value", "--:--").orEmpty()
        val rainPeriods = rainPeriodsFromRange(rainStart, rainEnd)
        val reminder = buildWeatherReminder(rainPeriods, maxTemp, minTemp, wind)

        return WeatherSummary(
            title = "新乡 · 河南科技学院",
            date = date,
            rainToday = rainToday,
            rainStart = rainStart,
            rainEnd = rainEnd,
            rainPeriodText = formatRainPeriods(rainPeriods),
            minTempC = minTemp,
            maxTempC = maxTemp,
            currentTempC = currentTemp,
            sunrise = sunrise,
            sunset = sunset,
            windSpeedMps = wind,
            reminder = reminder
        )
    }

    private fun closestHourlyIndex(hourly: JSONObject, todayDate: String): Int {
        val arr = hourly.optJSONArray("temperature") ?: return 0
        if (arr.length() == 0) return 0
        val now = LocalDateTime.now()
        var bestIndex = 0
        var bestDiff = Long.MAX_VALUE
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val dt = parseWeatherDateTime(item.optString("datetime", "").trim()) ?: continue
            if (dt.toLocalDate().toString() != todayDate) continue
            val diff = kotlin.math.abs(java.time.Duration.between(dt, now).seconds)
            if (diff < bestDiff) {
                bestDiff = diff
                bestIndex = i
            }
        }
        return bestIndex
    }

    private fun extractCaiyunSummary(weatherJson: JSONObject): WeatherSummary {
        val result = weatherJson.optJSONObject("result") ?: error("天气数据缺少 result")
        val hourly = result.optJSONObject("hourly") ?: error("天气数据缺少 hourly")
        val daily = result.optJSONObject("daily") ?: error("天气数据缺少 daily")
        val dailyTemp = daily.optJSONArray("temperature")?.optJSONObject(0) ?: error("天气数据缺少今日温度")
        val dateText = dailyTemp.optString("date", "").trim()
        val date = LocalDate.parse(dateText)
        val hi = closestHourlyIndex(hourly, dateText)
        val nowTemp = hourly.optJSONArray("temperature")?.optJSONObject(hi)?.optDouble("value", Double.NaN) ?: Double.NaN
        val windKmh = hourly.optJSONArray("wind")?.optJSONObject(hi)?.optDouble("speed", 0.0) ?: 0.0
        val wind = kmhToMps(windKmh)
        if (nowTemp.isNaN()) error("天气数据缺少当前温度")
        val minTemp = dailyTemp.optDouble("min", 0.0)
        val maxTemp = dailyTemp.optDouble("max", 0.0)

        val astro = daily.optJSONArray("astro")?.optJSONObject(0)
        val sunrise = trimToHm(astro?.optJSONObject("sunrise")?.optString("time", "").orEmpty())
        val sunset = trimToHm(astro?.optJSONObject("sunset")?.optString("time", "").orEmpty())

        val rainSlots = mutableListOf<String>()
        val precipitation = hourly.optJSONArray("precipitation") ?: JSONArray()
        for (i in 0 until precipitation.length()) {
            val item = precipitation.optJSONObject(i) ?: continue
            val dt = item.optString("datetime", "").trim()
            val value = item.optDouble("value", 0.0)
            if (dt.startsWith(dateText) && value > rainThresholdMm) {
                rainSlots += dt
            }
        }
        val rainToday = rainSlots.isNotEmpty()
        val rainPeriods = rainPeriodsFromSlots(rainSlots)

        return WeatherSummary(
            title = "新乡 · 河南科技学院",
            date = date,
            rainToday = rainToday,
            rainStart = rainSlots.firstOrNull()?.replace('T', ' '),
            rainEnd = rainSlots.lastOrNull()?.replace('T', ' '),
            rainPeriodText = formatRainPeriods(rainPeriods),
            minTempC = minTemp,
            maxTempC = maxTemp,
            currentTempC = nowTemp,
            sunrise = sunrise,
            sunset = sunset,
            windSpeedMps = wind,
            reminder = buildWeatherReminder(rainPeriods, maxTemp, minTemp, wind)
        )
    }

    private fun buildWeatherReminder(rainPeriods: List<String>, tempMax: Double, tempMin: Double, windMps: Double): String {
        val reminders = mutableListOf<String>()
        buildRainReminder(rainPeriods)?.let { reminders += it }
        val gap = tempMax - tempMin
        if (tempMax >= 30) {
            reminders += "白天偏热，注意防晒和补水(热鼠啦)🥵"
        } else if (tempMin <= 10) {
            if (tempMax >= 24 || gap >= 10) {
                reminders += "早晚偏凉但中午会热，建议穿件薄外套便于穿脱🧥"
            } else {
                reminders += "早晚偏凉，建议加一件外套哦🧥"
            }
        }
        if (windMps >= 10) reminders += "当前风力较大，注意高空坠物和出行安全呀💨"
        if (reminders.isEmpty()) reminders += "今天天气整体平稳，适合出行，gogogo出发喽🌤️"
        return reminders.joinToString(" ")
    }

    private fun buildRainReminder(rainPeriods: List<String>): String? {
        if (rainPeriods.isEmpty()) return null
        val rainText = formatRainPeriods(rainPeriods) ?: return null
        val currentIndex = rainPeriodIndex(labelForHour(LocalDateTime.now().hour))
        val nextRainIndex = rainPeriods
            .map { rainPeriodIndex(it) }
            .filter { it >= currentIndex }
            .minOrNull()
            ?: rainPeriods.minOf { rainPeriodIndex(it) }

        return when {
            nextRainIndex <= currentIndex -> "${rainText}有雨，外出记得带伞哦~🌧️"
            nextRainIndex - currentIndex == 1 -> "${rainText}有雨，接下来外出记得带伞哦~🌧️"
            else -> "${rainText}有雨，晚些外出留意降雨。"
        }
    }

    private fun kmhToMps(value: Double): Double = value / 3.6

    private fun parseWeatherDateTime(raw: String): LocalDateTime? {
        val s = raw.trim()
        if (s.isBlank()) return null
        return runCatching { LocalDateTime.parse(s) }.getOrNull()
            ?: runCatching { LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) }.getOrNull()
    }

    private fun extractHour(raw: String?): Int? {
        val s = raw?.trim().orEmpty()
        if (s.isBlank()) return null
        return Regex("(\\d{1,2}):(\\d{2})").findAll(s).lastOrNull()
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?.takeIf { it in 0..23 }
    }

    private fun labelForHour(hour: Int): String {
        return when (hour) {
            in 0..4 -> "凌晨"
            in 5..10 -> "早上"
            in 11..13 -> "中午"
            in 14..17 -> "下午"
            else -> "晚上"
        }
    }

    private fun rainPeriodIndex(label: String): Int {
        return rainPeriodOrder.indexOf(label).takeIf { it >= 0 } ?: Int.MAX_VALUE
    }

    private fun normalizeRainPeriods(labels: Collection<String>): List<String> {
        return labels
            .filter { it in rainPeriodOrder }
            .distinct()
            .sortedBy { rainPeriodIndex(it) }
    }

    private fun rainPeriodsFromRange(start: String?, end: String?): List<String> {
        val startHour = extractHour(start)
        val endHour = extractHour(end)
        if (startHour == null && endHour == null) return emptyList()
        if (startHour == null) return normalizeRainPeriods(listOf(labelForHour(endHour!!)))
        if (endHour == null) return normalizeRainPeriods(listOf(labelForHour(startHour)))

        val startIndex = rainPeriodIndex(labelForHour(startHour))
        val endIndex = rainPeriodIndex(labelForHour(endHour))
        val from = minOf(startIndex, endIndex)
        val to = maxOf(startIndex, endIndex)
        return rainPeriodOrder.subList(from, to + 1)
    }

    private fun rainPeriodsFromSlots(slots: List<String>): List<String> {
        return normalizeRainPeriods(slots.mapNotNull { extractHour(it)?.let(::labelForHour) })
    }

    private fun formatRainPeriods(periods: List<String>): String? {
        if (periods.isEmpty()) return null
        val normalized = normalizeRainPeriods(periods)
        if (normalized.isEmpty()) return null

        val groups = mutableListOf<List<String>>()
        var current = mutableListOf<String>()
        normalized.forEach { label ->
            if (current.isEmpty()) {
                current += label
                return@forEach
            }
            val prevIndex = rainPeriodIndex(current.last())
            val currentIndex = rainPeriodIndex(label)
            if (currentIndex == prevIndex + 1) {
                current += label
            } else {
                groups += current.toList()
                current = mutableListOf(label)
            }
        }
        if (current.isNotEmpty()) {
            groups += current.toList()
        }

        return groups.joinToString("、") { group ->
            if (group.size == 1) {
                group.first()
            } else {
                "${group.first()}到${group.last()}"
            }
        }
    }

    private fun trimToHm(raw: String): String {
        val s = raw.trim()
        if (s.isBlank()) return "--:--"
        val t = when {
            'T' in s -> s.substringAfter('T')
            ' ' in s -> s.substringAfter(' ')
            else -> s
        }
        return if (Regex("\\d{2}:\\d{2}").containsMatchIn(t)) {
            Regex("\\d{2}:\\d{2}").find(t)?.value ?: "--:--"
        } else {
            "--:--"
        }
    }

    private fun postText(
        url: String,
        headers: Map<String, String>,
        body: String,
        timeoutMs: Int
    ): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            doOutput = true
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        return try {
            conn.outputStream.use { out -> out.write(body.toByteArray(StandardCharsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = BufferedReader(stream.reader(StandardCharsets.UTF_8)).use { it.readText() }
            if (code !in 200..299) error("天气接口异常($code)")
            text
        } finally {
            conn.disconnect()
        }
    }

    fun queryFreeClassroomsData(
        config: CommonConfig,
        limit: Int,
        currentPeriodOverride: Int?,
        selectedBuilding: String? = null
    ): FreeRoomSchedule {
        val userId = normalizeUserId(config.userId, config.xxdm)
        val api = normalizeApiUrl(config.serviceUrl)

        val xnxq = currentXnxq(api, userId, config)
        val kb = fetchKbDetail(api, userId, xnxq, "", config)
        val now = LocalDateTime.now()
        val date = now.toLocalDate()
        val weekday = now.dayOfWeek.value

        val dayArr = kb.optJSONArray("week$weekday") ?: JSONArray()
        val dayCourses = (0 until dayArr.length()).mapNotNull { dayArr.optJSONObject(it) }

        val busy = mutableSetOf<Int>()
        dayCourses.forEach { c -> busy += parsePeriodsFromCourse(c) }

        val schoolMax = 10
        val userFree = (1..schoolMax).filterNot { it in busy }

        val periodCtx = if (currentPeriodOverride != null && currentPeriodOverride in 1..schoolMax) {
            PeriodContext(
                currentPeriod = currentPeriodOverride,
                nextPeriod = currentPeriodOverride,
                status = "手动指定第${currentPeriodOverride}节"
            )
        } else {
            resolvePeriodContext(now)
        }
        val estimated = periodCtx.currentPeriod ?: 0
        val basePeriod = periodCtx.currentPeriod ?: periodCtx.nextPeriod ?: schoolMax
        val target = basePeriod.coerceIn(1, schoolMax)
        val preferOrder = listOf("弘善楼", "弘毅楼")
        val anchor = detectAnchorBuilding(dayCourses, target)
        val selectedToken = selectedBuilding?.trim()?.takeIf { it.isNotBlank() }
        val defaultToken = anchor.takeIf { it.isNotBlank() }
        val activeToken = selectedToken ?: defaultToken

        val prioritizedJxls = prioritizeJxls(
            source = fetchJxlListCached(api, userId, config),
            preferOrder = preferOrder,
            anchor = anchor
        )

        val scanPlan = if (activeToken == null) {
            emptyList()
        } else {
            prioritizedJxls.filter { matchesBuildingSelection(extractBuildingToken(it.second), activeToken) }
        }

        val allRooms = mutableListOf<RoomCandidate>()
        val periodCount = mutableMapOf<Int, Int>()
        val activeScanToken = activeToken ?: ""
        val batchSize = 4
        var offset = 0
        while (offset < scanPlan.size) {
            val end = minOf(offset + batchSize, scanPlan.size)
            val batch = scanPlan.subList(offset, end)
            val result = scanBuildingsParallel(api, userId, date, batch, config, schoolMax)
            mergeScanResult(allRooms, periodCount, result)
            offset = end

            val relevantCount = allRooms.count { matchesBuildingSelection(extractBuildingToken(it.building), activeScanToken) }
            val shouldStop = relevantCount >= limit
            if (shouldStop) break
        }

        val availableBuildings = buildList {
            if (anchor.isNotBlank()) add(anchor)
            addAll(fixedBuildingOptions())
        }.distinct()

        val filteredRooms = if (activeToken == null) {
            allRooms
        } else {
            allRooms.filter { matchesBuildingSelection(extractBuildingToken(it.building), activeToken) }
        }
        val roomPool = filteredRooms

        val sorted = roomPool.sortedByDescending { scoreRoom(it, target, schoolMax, preferOrder, anchor) }
        val top = sorted.take(limit.coerceAtLeast(1))

        val roomItems = top.map { r ->
            val n = nextFreePeriod(r.freePeriods.toSet(), target, schoolMax) ?: target
            val run = contiguousRun(r.freePeriods.toSet(), n, schoolMax)
            FreeRoomItem(
                room = r.room,
                building = r.building,
                capacity = r.capacity,
                startPeriod = n,
                runLength = run,
                freePeriods = r.freePeriods
            )
        }

        return FreeRoomSchedule(
            date = date,
            weekday = weekday,
            term = xnxq,
            currentWeek = kb.optString("zc"),
            season = if (isSummer(date)) "夏令" else "冬令",
            estimatedPeriod = estimated,
            currentPeriod = periodCtx.currentPeriod,
            nextPeriod = periodCtx.nextPeriod,
            timeStatus = periodCtx.status,
            targetPeriod = target,
            anchorBuilding = anchor,
            activeBuilding = activeToken,
            availableBuildings = availableBuildings,
            busyPeriods = busy.sorted(),
            freePeriods = userFree,
            recommended = roomItems.firstOrNull(),
            rooms = roomItems
        )
    }

    fun queryFreeClassrooms(config: CommonConfig, limit: Int, currentPeriodOverride: Int?): String {
        val data = queryFreeClassroomsData(config, limit, currentPeriodOverride)
        return buildString {
            appendLine("日期: ${data.date}  周${data.weekday}")
            appendLine("学期: ${data.term}  当前周(接口): ${data.currentWeek}")
            appendLine("作息模式: ${data.season}")
            appendLine("优先级: 就近 > 连续刚空闲 > 楼层/楼宇偏好")
            appendLine("楼层策略: 1xx/2xx > 3xx；4xx/5xx已排除")
            appendLine("时间状态: ${data.timeStatus}；推荐目标第${data.targetPeriod}节")
            if (data.anchorBuilding.isNotBlank()) appendLine("就近锚点: ${data.anchorBuilding}（同楼 > 同片区 > 其他）")
            appendLine("------------------------------------------------------------")
            appendLine("你的有课节次: ${data.busyPeriods.joinToString(", ")}")
            appendLine("你的空档节次: ${data.freePeriods.joinToString(", ")}")
            appendLine("------------------------------------------------------------")
            if (data.rooms.isEmpty()) {
                appendLine("未找到符合规则的空教室。")
                return@buildString
            }

            val best = data.recommended ?: data.rooms.first()
            appendLine("推荐教室:")
            appendLine("${best.room}（${best.building}） 容量${best.capacity} 从第${best.startPeriod}节起连续${best.runLength}节")
            appendLine("------------------------------------------------------------")
            appendLine("可用空教室（Top ${data.rooms.size}）:")
            data.rooms.forEachIndexed { idx, r ->
                appendLine("${idx + 1}. ${r.room} | ${r.building} | 容量${r.capacity} | 从第${r.startPeriod}节起连续${r.runLength}节 | 全天可用:${r.freePeriods.joinToString(",")}")
            }
        }
    }

    private fun mergeScanResult(
        allRooms: MutableList<RoomCandidate>,
        periodCount: MutableMap<Int, Int>,
        result: BuildingScanResult
    ) {
        allRooms += result.rooms
        result.periodCount.forEach { (k, v) ->
            periodCount[k] = (periodCount[k] ?: 0) + v
        }
    }

    private fun scanBuildingsParallel(
        api: String,
        userId: String,
        date: LocalDate,
        jxls: List<Pair<String, String>>,
        cfg: CommonConfig,
        schoolMax: Int,
    ): BuildingScanResult {
        if (jxls.isEmpty()) return BuildingScanResult(emptyList(), emptyMap())
        val poolSize = minOf(10, maxOf(3, jxls.size))
        val executor = Executors.newFixedThreadPool(poolSize)
        try {
            val tasks = jxls.map { (dm, building) ->
                Callable {
                    scanSingleBuilding(api, userId, date, dm, building, cfg, schoolMax)
                }
            }
            val futures = executor.invokeAll(tasks, 9, TimeUnit.SECONDS)

            val allRooms = mutableListOf<RoomCandidate>()
            val periodCount = mutableMapOf<Int, Int>()
            futures.forEach { f ->
                if (f.isCancelled) return@forEach
                try {
                    val result = f.get() ?: return@forEach
                    mergeScanResult(allRooms, periodCount, result)
                } catch (_: Exception) {
                }
            }
            return BuildingScanResult(allRooms, periodCount)
        } finally {
            executor.shutdownNow()
        }
    }

    private fun scanSingleBuilding(
        api: String,
        userId: String,
        date: LocalDate,
        dm: String,
        building: String,
        cfg: CommonConfig,
        schoolMax: Int,
    ): BuildingScanResult? {
        val detail = fetchJxlDetailCached(api, userId, date.toString(), dm, cfg) ?: return null
        val result = detail.optJSONObject("result") ?: return null
        val jsxx = result.optJSONArray("jsxx") ?: return null

        val rooms = mutableListOf<RoomCandidate>()
        val periodCount = mutableMapOf<Int, Int>()
        for (i in 0 until jsxx.length()) {
            val row = jsxx.optJSONObject(i) ?: continue
            val room = row.optString("jsmc", "").trim()
            val roomType = row.optString("jslxmc", "").trim()
            val cap = Regex("\\d+").find(row.optString("jsrl", ""))?.value?.toIntOrNull() ?: 0
            val rawStatus = row.optString("sfkx", "").split(',').map { it.trim() }

            if (room.isBlank()) continue
            if (isOutdoor(room, roomType, building)) continue
            if (isExcludedHejiao(room)) continue
            if (isExcludedFloor(room)) continue

            val freePeriods = mutableListOf<Int>()
            rawStatus.forEachIndexed { idx, code ->
                val p = idx + 1
                if (p > schoolMax) return@forEachIndexed
                if (code == "1") {
                    freePeriods += p
                    periodCount[p] = (periodCount[p] ?: 0) + 1
                }
            }
            if (freePeriods.isEmpty()) continue
            rooms += RoomCandidate(room, building, cap, roomType, freePeriods.sorted(), rawStatus)
        }
        return BuildingScanResult(rooms, periodCount)
    }

    private fun prioritizeJxls(
        source: List<Pair<String, String>>,
        preferOrder: List<String>,
        anchor: String
    ): List<Pair<String, String>> {
        return source.sortedByDescending { (_, building) ->
            val roomBuilding = extractBuildingToken(building)
            var score = 0
            score += buildingOrderBonus(building, preferOrder)
            score += proximityBonus(roomBuilding, anchor)
            score
        }
    }

    private fun fixedBuildingOptions(): List<String> {
        return listOf(
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
    }

    private fun matchesBuildingSelection(roomBuildingToken: String, selected: String): Boolean {
        return when (selected) {
            "弘善楼", "弘毅楼" -> roomBuildingToken == selected
            "西校区(东)3号楼" -> roomBuildingToken == "3号楼"
            "西校区(东)4号楼" -> roomBuildingToken == "4号楼"
            "西校区(东)5号楼" -> roomBuildingToken == "5号楼"
            "西校区(东)6号楼" -> roomBuildingToken == "6号楼"
            "西校区(东)7号楼" -> roomBuildingToken == "7号楼"
            "西校区(东)8号楼" -> roomBuildingToken == "8号楼"
            "西校区(东)9号楼" -> roomBuildingToken == "9号楼"
            "西校区(东)10号楼" -> roomBuildingToken == "10号楼"
            "西校区(东)11号楼" -> roomBuildingToken == "11号楼"
            "西校区(西)0号楼" -> roomBuildingToken == "0号楼"
            "西校区(西)1号楼" -> roomBuildingToken == "1号楼"
            "西校区(西)2号楼" -> roomBuildingToken == "2号楼"
            else -> roomBuildingToken == selected
        }
    }

    private fun sortBuildingTokens(
        buildings: List<String>,
        preferOrder: List<String>,
        anchor: String
    ): List<String> {
        return buildings.sortedByDescending { b ->
            var score = 0
            score += buildingOrderBonus(b, preferOrder)
            score += proximityBonus(b, anchor)
            score
        }
    }

    private fun fetchJxlListCached(api: String, userId: String, cfg: CommonConfig): List<Pair<String, String>> {
        val now = System.currentTimeMillis()
        synchronized(cacheLock) {
            if (jxlListCache.isNotEmpty() && now - jxlListCacheAt < 6 * 60 * 60 * 1000L) {
                return jxlListCache
            }
        }
        val fresh = fetchJxlList(api, userId, cfg)
        synchronized(cacheLock) {
            jxlListCache = fresh
            jxlListCacheAt = now
        }
        return fresh
    }

    private fun fetchJxlDetailCached(
        api: String,
        userId: String,
        date: String,
        dm: String,
        cfg: CommonConfig
    ): JSONObject? {
        val now = System.currentTimeMillis()
        val key = "${cfg.xxdm}|$date|$dm"
        synchronized(cacheLock) {
            val hit = detailCache[key]
            if (hit != null && now - hit.at < detailCacheTtlMs) {
                return hit.detail
            }
        }
        val fresh = fetchJxlDetail(api, userId, date, dm, cfg)
        synchronized(cacheLock) {
            detailCache[key] = DetailCacheEntry(now, fresh)
            if (detailCache.size > 300) {
                val threshold = now - detailCacheTtlMs
                detailCache.entries.removeIf { it.value.at < threshold }
            }
        }
        return fresh
    }

    private fun fetchJxlList(api: String, userId: String, cfg: CommonConfig): List<Pair<String, String>> {
        val pairs = linkedMapOf(
            "userId" to userId,
            "action" to "oriKxjs",
            "step" to "jxl",
            "flag" to "1",
        )
        val j = postEncryptedJson(api, pairs, cfg) ?: return emptyList()
        val arr = j.optJSONArray("resultSet") ?: return emptyList()
        return (0 until arr.length()).mapNotNull {
            val o = arr.optJSONObject(it) ?: return@mapNotNull null
            val dm = o.optString("dm", "").trim()
            val mc = o.optString("mc", "").trim()
            if (dm.isBlank() || mc.isBlank()) null else dm to mc
        }
    }

    private fun fetchJxlDetail(api: String, userId: String, date: String, dm: String, cfg: CommonConfig): JSONObject? {
        val p = linkedMapOf(
            "userId" to userId,
            "usertype" to cfg.userType,
            "action" to "oriKxjs",
            "step" to "detail",
            "rq" to date,
            "jxl" to dm,
        )
        return postEncryptedJson(api, p, cfg)
    }

    private fun parsePeriodsFromCourse(c: JSONObject): Set<Int> {
        val out = mutableSetOf<Int>()
        val jcdm = c.optString("jcdm", "")
        if (jcdm.isNotBlank()) {
            jcdm.split(',').forEach { x -> x.trim().toIntOrNull()?.let { out += it } }
        }
        val jcxx = c.optString("jcxx", "")
        Regex("(\\d+)\\s*-\\s*(\\d+)").findAll(jcxx).forEach {
            val a = it.groupValues[1].toIntOrNull() ?: return@forEach
            val b = it.groupValues[2].toIntOrNull() ?: return@forEach
            if (a <= b) out += (a..b)
        }
        if (out.isEmpty()) {
            Regex("\\d+").findAll(jcxx).forEach { m -> out += (m.value.toIntOrNull() ?: 0) }
        }
        return out.filter { it > 0 }.toSet()
    }

    private fun randomEcho(): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
        return buildString {
            repeat(16) { append(chars.random()) }
        }
    }

    private fun isOutdoor(room: String, roomType: String, building: String): Boolean {
        val s = "$room $roomType $building"
        return listOf("室外", "体育场", "实验田", "田间").any { it in s }
    }

    private fun isExcludedHejiao(room: String): Boolean {
        val s = room.replace(" ", "")
        return listOf("合四", "合五", "#合四", "#合五", "合教四", "合教五").any { it in s }
    }

    private fun floorLevel(room: String): Int? {
        val m = Regex("[A-Za-z#]?\\s*(\\d{3})").find(room) ?: return null
        val n = m.groupValues[1].toIntOrNull() ?: return null
        val level = n / 100
        return if (level in 1..9) level else null
    }

    private fun isExcludedFloor(room: String): Boolean {
        return floorLevel(room) in setOf(4, 5)
    }

    private fun floorBonus(room: String): Int {
        return when (floorLevel(room)) {
            1, 2 -> 2600
            3 -> 300
            else -> 0
        }
    }

    private fun roomTypeBonus(roomType: String): Int {
        var b = 0
        if ("多媒体" in roomType || "合教" in roomType || "教室" in roomType) b += 60
        if ("实验室" in roomType) b -= 20
        if ("专用" in roomType) b -= 10
        return b
    }

    private fun extractBuildingToken(text: String): String {
        val s = text.trim()
        if (s.isBlank()) return ""
        listOf("弘善楼", "弘毅楼", "弘德楼").forEach { if (it in s) return it }
        val m = Regex("((?:10|11|[0-9]))\\s*[#号]?\\s*楼").find(s)
        if (m != null) return "${m.groupValues[1]}号楼"
        return ""
    }

    private fun cluster(token: String): String = when (token) {
        "弘善楼", "弘毅楼", "弘德楼" -> "A"
        "3号楼", "4号楼", "5号楼" -> "B"
        "6号楼", "7号楼", "8号楼", "9号楼", "10号楼", "11号楼" -> "C"
        "0号楼", "1号楼", "2号楼" -> "D"
        else -> ""
    }

    private fun proximityBonus(roomBuilding: String, anchorBuilding: String): Int {
        if (anchorBuilding.isBlank()) return 0
        if (roomBuilding == anchorBuilding) return 7600
        val c1 = cluster(roomBuilding)
        val c2 = cluster(anchorBuilding)
        if (c1.isNotBlank() && c1 == c2) return 2800
        return 0
    }

    private fun buildingOrderBonus(building: String, order: List<String>): Int {
        val total = order.size
        order.forEachIndexed { i, kw -> if (kw.isNotBlank() && kw in building) return (total - i) * 900 }
        return 0
    }

    private fun detectAnchorBuilding(dayCourses: List<JSONObject>, targetPeriod: Int): String {
        var bestP = -1
        var best = ""
        for (c in dayCourses) {
            val periods = parsePeriodsFromCourse(c).sorted()
            val prev = periods.filter { it < targetPeriod }
            if (prev.isEmpty()) continue
            val p = prev.last()
            val b = extractBuildingToken(c.optString("skdd", ""))
            if (b.isBlank()) continue
            if (p > bestP) {
                bestP = p
                best = b
            }
        }
        return best
    }

    private fun nextFreePeriod(free: Set<Int>, start: Int, maxP: Int): Int? {
        for (p in start..maxP) if (p in free) return p
        return null
    }

    private fun contiguousRun(free: Set<Int>, start: Int, maxP: Int): Int {
        if (start !in free) return 0
        var p = start
        var n = 0
        while (p <= maxP && p in free) {
            n += 1
            p += 1
        }
        return n
    }

    private fun lastOccupiedBefore(raw: List<String>, period: Int): Int {
        val n = minOf(period - 1, raw.size)
        for (p in n downTo 1) {
            if (raw[p - 1] != "1") return p
        }
        return 0
    }

    private fun scoreRoom(r: RoomCandidate, targetPeriod: Int, maxP: Int, order: List<String>, anchor: String): Double {
        val freeSet = r.freePeriods.toSet()
        val next = nextFreePeriod(freeSet, targetPeriod, maxP) ?: return -1e9
        val wait = (next - targetPeriod).coerceAtLeast(0)
        val run = contiguousRun(freeSet, next, maxP)
        val prev = lastOccupiedBefore(r.rawStatus, next)

        val recentBonus = when {
            prev == next - 1 && prev > 0 -> 2600
            prev > 0 -> maxOf(450, 1850 - 260 * (next - prev - 1))
            else -> -1200
        }

        var s = 0.0
        s += buildingOrderBonus(r.building, order)
        s += proximityBonus(extractBuildingToken(r.building), anchor)
        s += floorBonus(r.room)
        s += roomTypeBonus(r.roomType)
        s += run * 420
        s += r.freePeriods.size * 8
        s += minOf(r.capacity, 200) * 0.08
        s -= wait * 360
        s += recentBonus
        return s
    }

    private fun isSummer(date: LocalDate): Boolean {
        val md = date.monthValue * 100 + date.dayOfMonth
        return md in 501..930
    }

    private fun periodTable(date: LocalDate): LinkedHashMap<Int, Pair<Int, Int>> {
        return if (isSummer(date)) {
            linkedMapOf(
                1 to (8 * 60 to 8 * 60 + 45),
                2 to (8 * 60 + 55 to 9 * 60 + 40),
                3 to (10 * 60 + 10 to 10 * 60 + 55),
                4 to (11 * 60 + 5 to 11 * 60 + 50),
                5 to (15 * 60 to 15 * 60 + 45),
                6 to (15 * 60 + 55 to 16 * 60 + 40),
                7 to (17 * 60 + 10 to 17 * 60 + 55),
                8 to (18 * 60 + 5 to 18 * 60 + 50),
                9 to (20 * 60 to 20 * 60 + 45),
                10 to (20 * 60 + 55 to 21 * 60 + 40),
            )
        } else {
            linkedMapOf(
                1 to (8 * 60 to 8 * 60 + 45),
                2 to (8 * 60 + 55 to 9 * 60 + 40),
                3 to (10 * 60 + 10 to 10 * 60 + 55),
                4 to (11 * 60 + 5 to 11 * 60 + 50),
                5 to (14 * 60 + 30 to 15 * 60 + 15),
                6 to (15 * 60 + 25 to 16 * 60 + 10),
                7 to (16 * 60 + 40 to 17 * 60 + 25),
                8 to (17 * 60 + 35 to 18 * 60 + 20),
                9 to (19 * 60 + 30 to 20 * 60 + 15),
                10 to (20 * 60 + 25 to 21 * 60 + 10),
            )
        }
    }

    private fun resolvePeriodContext(now: LocalDateTime): PeriodContext {
        val mins = now.hour * 60 + now.minute
        val table = periodTable(now.toLocalDate())
        val periods = table.keys.toList().sorted()
        val first = periods.firstOrNull() ?: return PeriodContext(null, null, "休息中")
        val last = periods.lastOrNull() ?: return PeriodContext(null, null, "休息中")
        val firstStart = table[first]?.first ?: 8 * 60
        val lastEnd = table[last]?.second ?: 21 * 60
        if (mins < firstStart) return PeriodContext(null, first, "休息中（未开课）")
        if (mins >= lastEnd) return PeriodContext(null, null, "休息中（已下课）")

        for (idx in periods.indices) {
            val p = periods[idx]
            val st = table[p]?.first ?: continue
            val ed = table[p]?.second ?: continue
            if (mins in st until ed) {
                return PeriodContext(p, p, "第${p}节上课中")
            }
            val next = periods.getOrNull(idx + 1)
            val nextStart = if (next == null) Int.MAX_VALUE else (table[next]?.first ?: Int.MAX_VALUE)
            if (mins in ed until nextStart) {
                return PeriodContext(null, next, "休息中")
            }
        }
        return PeriodContext(null, null, "休息中")
    }
}
