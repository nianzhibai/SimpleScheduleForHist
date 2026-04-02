# CleanKB Android

一个独立实现的安卓课表助手（Jetpack Compose），包含：
- 今日课表查询
- 本周课表查询
- 空教室推荐（按你的偏好规则动态推荐）
- 校园天气卡片

## 功能特点
- 使用学校接口实时拉取课表数据
- 首次启动仅需录入学号，后续会保存在本地
- 自动按时间判断当前节次（夏令/冬令）
- 顶部天气卡片会展示当天温度、日出日落和出行提醒
- 空教室推荐规则已内置：
  - 楼宇偏好：弘善楼 > 弘毅楼
  - 就近推荐：同楼 > 同片区 > 其他
  - 连续空闲优先，且优先“刚空出来”的教室
  - 楼层偏好：1xx/2xx > 3xx；过滤 4xx/5xx
  - 过滤：合四/合五、室外场地
- 空教室页面支持按楼栋筛选和拖动调整楼栋排序偏好

## 开发环境
- Android Studio Hedgehog 及以上（建议最新稳定版）
- Android SDK 35
- JDK 17

## 运行方式
1. 用 Android Studio 打开目录 `clean-kb-android`
2. 等待 Gradle 同步完成
3. 连接真机或启动模拟器
4. 点击 Run 运行 `app`

## 首次使用
首次打开时只需要填写学号。

说明：
- 首次安装打开时会强制弹窗输入学号，保存后才能继续使用
- 学号会保存到本地，后续打开不需要重复输入
- 当前版本服务地址、学校代码、用户类型和空教室数量都在代码中固定：
  - 服务地址：`https://api.xiqueer.com/manager/`
  - 学校代码：`10467`
  - 用户类型：`STU`
  - 空教室推荐条数：`6`
- 如需修改上述固定值，可调整 `app/src/main/java/com/cleankb/app/MainActivity.kt` 中的默认常量

## 项目结构
- `app/src/main/java/com/cleankb/app/MainActivity.kt`：Compose UI、页面状态与请求调度
- `app/src/main/java/com/cleankb/app/data/CampusService.kt`：课表、天气、空教室查询与推荐逻辑
- `app/src/main/java/com/cleankb/app/ui/theme`：主题与配色
