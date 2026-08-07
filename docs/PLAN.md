# 「自我档案与人物图鉴」开发实施计划

> **版本**：v1.0  
> **状态**：Active  
> **最后更新**：2026-08-07  
> **配套文档**：[PRD.md](./PRD.md) · `design/`（HTML 原型）

## 0. 关键决策（已与用户对齐）

| 项 | 决策 | 备注 |
|---|---|---|
| **目标平台** | Android only | 不做 iOS |
| **开发语言** | Kotlin | 用户主语言背景 |
| **UI 框架** | Jetpack Compose（Material 3） | 不用老 XML View 体系 |
| **MVP 范围** | PRD Phase 1 全量 | 5 Tab + 14 自我模块 + 人物图鉴 + 关系图谱 + 提醒中心 + 本地通知 + 模块/人物拖拽 + 应用锁基础版 |
| **工程节奏** | 边搭边学型 | 先骨架 + 首页/设置跑通，再按页面迭代 |
| **云端** | 不做 | PRD 明确 non-goal，所有数据本地 |
| **第三方账号** | 不做 | PRD 明确 non-goal |

## 1. 技术栈与依赖

### 1.1 核心栈

| 层 | 选型 | 理由 |
|---|---|---|
| **UI** | Jetpack Compose（BOM 2024.06+） + Material 3 | 声明式 UI 适合手账风格自定义；与原生 Material 3 控件一致 |
| **导航** | Navigation Compose 2.7+ | Tab + 详情页两级路由足够；后续可上 type-safe navigation |
| **状态** | ViewModel + StateFlow | 标配方案 |
| **DI** | Hilt | 项目复杂度提升后值得引入，初期也可手写单例 |
| **数据库** | Room 2.6+ | Person / Reminder / Settings 的关系型存储 |
| **键值偏好** | DataStore Preferences | 替代 SharedPreferences；深色模式/字体大小/锁屏等开关项 |
| **图片** | Coil 2.6+ | Compose 集成友好，照片墙 + 头像 |
| **本地通知** | WorkManager + NotificationCompat | 提醒调度（不依赖 FCM，本地通知） |
| **生物识别** | AndroidX Biometric | 应用锁 |
| **JSON 序列化** | kotlinx-serialization | 数据导出/导入 |
| **日期** | java.time（API 26+）+ 农历用 `lunar-java` | minSdk 26 直接用 java.time |
| **拖拽** | Compose 1.6 原生 `dragAndDropSource/Target` + `reorderable` 库辅助 | 满足 US-016 / US-017 |
| **关系图谱** | Compose Canvas + 手势识别 | 不引入额外库，纯绘制 |
| **图表/动画** | Compose 内置 `animate*AsState` / `InfiniteTransition` | 中心节点 pulse 动画 |

### 1.2 Gradle 配置

| 配置 | 值 | 备注 |
|---|---|---|
| AGP | 8.5+ | 兼容 Kotlin 2.0 |
| Kotlin | 2.0+ | 启用 Compose Compiler Plugin |
| compileSdk | 34 | Android 14 |
| minSdk | 26 | Android 8.0，覆盖 95%+ 设备，java.time 可用 |
| targetSdk | 34 | 适配最新 |
| Java | 17 | AGP 8.x 要求 |

### 1.3 第三方库版本（初步，后续以实际更新为准）

```kotlin
// build.gradle.kts (app) 核心依赖
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // 图片
    implementation("io.coil-kt:coil-compose:2.6.0")

    // WorkManager + Hilt
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // 生物识别
    implementation("androidx.biometric:biometric:1.1.0")

    // 序列化
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // 农历
    implementation("cn.6tail:lunar-java:1.6.x")  // 实际查 Maven 最新稳定版
}
```

## 2. 工程结构

```
self/                                    # Android Studio 工程根（直接打开本目录）
├── app/                                  # app 模块
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/self/profile/
│       │   │   ├── SelfApp.kt            # Application
│       │   │   ├── MainActivity.kt
│       │   │   ├── di/                   # Hilt modules
│       │   │   ├── data/
│       │   │   │   ├── local/            # Room
│       │   │   │   │   ├── AppDatabase.kt
│       │   │   │   │   ├── entity/       # PersonEntity, ReminderEntity
│       │   │   │   │   ├── dao/
│       │   │   │   │   └── converter/    # TypeConverter
│       │   │   │   ├── datastore/        # SettingsDataStore
│       │   │   │   ├── repository/       # PersonRepo, ReminderRepo, SettingsRepo
│       │   │   │   └── seed/             # 首次启动种子数据
│       │   │   ├── domain/
│       │   │   │   ├── model/            # Person, Reminder, AppSettings
│       │   │   │   ├── usecase/          # 业务逻辑（可选）
│       │   │   │   └── service/          # ReminderScheduler
│       │   │   ├── ui/
│       │   │   │   ├── theme/            # Color.kt, Type.kt, Theme.kt, Shape.kt
│       │   │   │   ├── components/       # 共享组件
│       │   │   │   ├── navigation/       # AppNav.kt, Route.kt, BottomBar.kt
│       │   │   │   ├── home/             # 首页
│       │   │   │   ├── self/             # 我的档案（14 模块）
│       │   │   │   ├── people/           # 人物图鉴
│       │   │   │   ├── map/              # 关系图谱
│       │   │   │   └── settings/         # 设置/提醒中心
│       │   │   ├── util/                 # DateUtils, LunarHelper, QuoteProvider
│       │   │   └── work/                 # ReminderWorker, ReminderScheduler
│       │   └── res/
│       │       ├── values/               # colors.xml, strings.xml, themes.xml
│       │       ├── values-night/         # 深色主题
│       │       ├── drawable/             # 图标资源
│       │       ├── mipmap-anydpi-v26/    # 应用图标（自适应）
│       │       └── xml/                  # 备份规则等
│       ├── test/                         # 单元测试
│       └── androidTest/                  # UI 测试（可选）
├── gradle/                               # Gradle wrapper
│   ├── libs.versions.toml                # 依赖版本目录
│   └── wrapper/
├── docs/
│   ├── PRD.md                            # 已有
│   └── PLAN.md                           # 本文件
├── design/                               # 已有 HTML 原型
├── .archive/                             # 历史归档（不参与编译）
├── build.gradle.kts                      # 根 Gradle
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

> **包名**：`com.self.profile`（应用名 self，包名加 profile 避免冲突）

## 3. 数据模型落地

PRD §3.2 已经定好 JSON 结构，对应到 Room Entity：

### 3.1 `PersonEntity`

```kotlin
@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey val id: String,                 // UUID
    val name: String,
    val avatarColor: String,                    // #RRGGBB
    val avatarInitial: String,                  // 姓氏首字
    val relationshipType: String,               // 家人/朋友/同事/同学
    val mbti: String?,
    val bloodType: String?,
    val closenessLevel: String,                 // 近/中/远
    val isSelf: Boolean,
    @Embedded val personalBackground: PersonalBackgroundEmbed?,
    @Embedded val bodyData: BodyDataEmbed?,
    @Embedded val sizeInfo: SizeInfoEmbed?,
    // 嵌套数组用 TypeConverter 转 JSON 字符串
    val thingsLikedJson: String,                // [{category, items:[]}]
    val thingsDislikedJson: String,
    val personalityAnalysis: String?,
    val idealType: String?,
    val aspirationsJson: String,                // [{id, content, createdAt}]
    val wishListJson: String,                   // [{id, content, completed, createdAt}]
    val lifeJourneyJson: String,                // [{id, year, title, type, description}]
    val thoughtNotesJson: String,               // 仅 isSelf=true
    val interactionNotesJson: String,           // 仅 isSelf=false
    val commonGroundsJson: String,              // 仅 isSelf=false
    val photosJson: String,
    val anniversariesJson: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastViewedAt: Long?
)
```

> **取舍说明**：嵌套数组用 `kotlinx-serialization` 转 JSON 存进单字段，而不是每个子项建一张关联表。原因：MVP 阶段这些数据规模小（人均 < 50 条愿望/笔记），不需要 JOIN；后续真有必要再迁到关联表。PRD §4.2 的 schema version 策略预留好就行。

### 3.2 `ReminderEntity`

```kotlin
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val personId: String?,                      // null = 自定义提醒
    val type: String,                           // 生日/纪念日/自定义
    val title: String,
    val date: Long,                             // ISO date as epoch day
    val advanceDays: Int,
    val reminderTime: String,                   // "HH:mm"
    val repeat: String,                         // 不重复/每年/每月/每周
    val enabled: Boolean,
    val notes: String?,
    val triggeredAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
)
```

### 3.3 应用设置

- **DataStore Preferences** 存简单开关/单值（深色模式、字体大小、锁屏开关、默认提前天数等）
- **Person 模块顺序** 等数组类复杂数据也用 DataStore + JSON 字符串

```kotlin
// 关键字段
userNickname: String = "用户"
defaultAdvanceDays: Int = 3
defaultReminderTime: String = "08:00"
showLunarBirthday: Boolean = false
darkMode: String = "system"        // off/on/system
fontScale: String = "standard"     // small/standard/large/xlarge
selfProfileModuleOrder: String = "[1,2,3,...]"
personDetailModuleOrder: String = "[1,2,3,...]"
gallerySortRule: String = "created_at"
galleryCustomOrder: String? = null
appLockEnabled: Boolean = false
appLockTimeout: Int = 5
```

## 4. 实施阶段

按"边搭边学"节奏，分 7 个里程碑，每个里程碑结束都能在你手机上**装一个能用的版本**。

### 阶段 0：项目骨架（1-2 天）

**目标**：在 Android Studio 中能编译并跑起来，看到一个空壳首页。

- [ ] 生成工程目录结构（app/、gradle/、docs/ 同级）
- [ ] 写 `build.gradle.kts` (project + app) + `settings.gradle.kts` + `libs.versions.toml` + `gradle.properties`
- [ ] 配 `gradle wrapper`（gradle 8.7+）
- [ ] 配 `AndroidManifest.xml`（Application、Activity、权限占位）
- [ ] 建 `SelfApp.kt` + `MainActivity.kt` + 顶层 `AppNav.kt` 路由
- [ ] 建 `theme/Color.kt` + `Type.kt` + `Theme.kt`（从 `design/colors_and_type.css` 翻译过来）
- [ ] 建 5 个 Tab 的占位 Screen（每个一行文字 "首页 / 档案 / 图鉴 / 图谱 / 设置"）
- [ ] 底部 `NavigationBar` 组件
- [ ] Gradle sync + 编译通过 + 在模拟器/真机跑通空白骨架

**验收**：能装到手机，看到 5 个 Tab 切换。

### 阶段 1：数据层 + 5 Tab 静态 UI（3-5 天）

**目标**：数据层跑通，5 Tab 都有静态内容（用种子数据）。

- [ ] Room：`AppDatabase` + `PersonDao` + `ReminderDao` + `SettingsDataStore` + Entity
- [ ] 种子数据：首次启动写入示例 `Person`（包含 1 个 self + 5 个 friends/family/colleagues）+ 3 条 Reminder
- [ ] `PersonRepository` / `ReminderRepository` / `SettingsRepository` 暴露 Flow
- [ ] 首页：问候语 + 4 快捷入口 + 近期提醒列表 + 关系概览 + 今日寄语
- [ ] 我的档案：14 模块全部渲染（只读，能看不能改）
- [ ] 人物图鉴：2 列网格 + 搜索 + 筛选
- [ ] 关系图谱：Canvas 绘制（参考原型的 3 圈轨道 + 中心 pulse + 外围节点）
- [ ] 设置：4 个区块静态展示（提醒中心、快捷设置、应用设置、关于）
- [ ] 顶部状态栏适配（沉浸式）
- [ ] 安全区处理（NavigationBar insets）

**验收**：5 Tab 都能进，能看到种子数据，所有静态 UI 跟原型一致。

### 阶段 2：自我档案编辑（3-4 天）

- [ ] 14 模块全部支持点击进入编辑（弹窗或 inline）
- [ ] 头像上传（相册 + 裁剪）
- [ ] MBTI 选择器、血型选择器、关系类型选择器等通用 picker
- [ ] 编辑后自动保存（debounce 500ms 落库）
- [ ] 农历生日自动计算（基于 `lunar-java`）
- [ ] 星座自动计算（基于生日）
- [ ] 提醒铃铛按钮 → 弹出 ReminderForm（提前天数、时间、重复、备注）
- [ ] 提醒配置保存后立刻调度 WorkManager

**验收**：所有字段都能改、能保存、能关联到提醒。

### 阶段 3：人物图鉴交互（3-4 天）

- [ ] 搜索实时过滤（输入 200ms 内响应）
- [ ] 分类筛选
- [ ] FAB 添加新人物（bottom sheet 表单）
- [ ] 必填校验（姓名为空时按钮置灰 + 提示）
- [ ] 人物详情页（与自我档案同样的 14 模块结构 + 互动记录 + 共同点）
- [ ] 删除/编辑人物
- [ ] US-017 排序：4 种规则（最近查看、最近修改、创建时间、自定义）

**验收**：能新建、编辑、删除人物，能按 4 种规则排序。

### 阶段 4：关系图谱完善（2-3 天）

- [ ] 节点距离映射 closeness（家人内圈 / 朋友中圈 / 同事同学外圈）
- [ ] 连线样式区分（实线/虚线/点线，按关系类型）
- [ ] 中心 pulse 动画（InfiniteTransition 2s 周期）
- [ ] 节点点击跳转详情
- [ ] 2x2 统计网格
- [ ] 空状态引导

**验收**：跟原型一致，节点可点击。

### 阶段 5：提醒中心 + 本地通知（3-4 天）

- [ ] 提醒中心 5 Tab 布局
- [ ] 统计概览（已设置/已开启/7 天内即将到来）
- [ ] 即将到来列表（最多 5 条）
- [ ] 全部提醒列表 + 类型筛选
- [ ] Toggle 即时开关
- [ ] 编辑/删除
- [ ] WorkManager 调度
- [ ] NotificationCompat 通知样式
- [ ] 应用启动时重注册所有未过期提醒
- [ ] 重复逻辑（每年/每月/每周的 next trigger 计算）

**验收**：创建一个生日提醒 → 在预期时间收到通知。

### 阶段 6：模块拖拽排序 + 高级功能（3-4 天）

- [ ] US-016：自我档案模块长按拖拽排序 + 持久化
- [ ] US-017：人物图鉴自定义排序模式
- [ ] US-018：应用锁（基础版）
  - 开关 + 默认设置
  - 冷启动 + 后台 5 秒后回前台触发
  - BiometricPrompt
  - 失败次数处理（生物识别 3 次 → 降级到 PIN）
- [ ] 深色模式（系统/亮/暗 三档，Theme 切换）
- [ ] 字体大小（标准/大号）
- [ ] 数据导出（JSON，写入 Downloads 目录）

**验收**：所有 US 故事跑通，PRD Phase 1 全量完成。

### 阶段 7：收尾与自测（2-3 天）

- [ ] 空状态全检查
- [ ] 长按 / 多选等手势无冲突
- [ ] 退到后台再回来无状态丢失
- [ ] 删除全部数据后重启正常
- [ ] 大数据量（50 个人物 + 200 条提醒）性能检查
- [ ] 横屏/折叠屏适配（最低限度）
- [ ] ProGuard 规则
- [ ] App 图标 + 启动屏

**验收**：能作为日常应用连续用一周不出 bug。

## 5. 关键设计决定与 Trade-off

### 5.1 嵌套数据用 JSON 字段 vs 关联表

- **选 JSON 字段**：MVP 阶段数据量小（愿望清单人均 < 50 条），少 N 张表、少 N 个 DAO、少 N 个 Repository
- **代价**：不能 SQL JOIN 复杂查询；后续如果要做"按年份统计愿望完成率"这种聚合，要先在内存反序列化
- **降级路径**：schema version 升级时把 JSON 字段迁到独立表，DAO 透明升级

### 5.2 Compose Canvas 画关系图谱

- **不用 SVG 库**：原型是 SVG，但 Compose Canvas 性能更好、手势识别更顺
- **参考实现**：完全照搬原型的 3 圈轨道 + 节点位置（基于 closeness 的极坐标）
- **节点位置算法**：
  - 家人：内圈 r=85，均匀分布
  - 朋友：中圈 r=128
  - 同事/同学：外圈 r=153，混合均匀分布

### 5.3 提醒调度方案

- **WorkManager** 而不是 AlarmManager：理由是 WorkManager 在 Doze 模式下更可靠，且能处理约束条件
- **一次性 + 重新调度**：触发后按 `repeat` 规则算下次时间，重新 enqueue
- **冷启动重注册**：应用启动时遍历所有 enabled reminder，按 next trigger 重新调度

### 5.4 应用锁方案

- **不存密码**：完全依赖系统生物识别 / 设备 PIN
- **背景时间记录**：用 `ProcessLifecycleOwner` 监听 ON_START，在 ViewModel 中比较上次 ON_STOP 时间
- **锁屏页**：用 Compose 单独一个 Activity 启动 / 用 NavHost 控制，倾向后者（更顺滑）

### 5.5 中文国际化

- **v1 只做中文**（跟设计稿一致）；strings.xml 命名走 zh-CN
- 预留英文资源路径（`values-en/`），但内容先空着

## 6. 风险与缓解

| 风险 | 影响 | 缓解 |
|---|---|---|
| Compose 1.6 拖拽 API 学习曲线 | 中 | 阶段 6 集中处理；优先用社区 `reorderable` 库 |
| WorkManager 在某些 OEM 上被杀 | 中 | 同时注册 AlarmManager 作为兜底（仅一次性 + 不可重复的提醒） |
| 农历计算精度 | 低 | 用 `lunar-java`（覆盖 1900-2100 年，足够个人使用） |
| 数据库迁移 | 中 | Room Migration 加 fallbackToDestructiveMigration（MVP 阶段可接受） |
| 性能（关系图谱 > 50 人） | 低 | 阶段 7 验证；超 50 人时考虑分层渲染 |
| Android 14 部分行为变更 | 中 | targetSdk 34 适配前重点查 Foreground Service / 通知权限变化 |

## 7. 不在本阶段做的事

- iOS 版本（用户明确只做 Android）
- 云同步、AI 分析、社交分享（PRD 明确 non-goal）
- PRD Phase 2 全部（深色模式除外，基础版要做）/ Phase 3
- 单元测试覆盖率指标（MVP 阶段以"核心业务方法 + 农历计算"为主）

## 8. 验收标准（Phase 1 完成时）

- [ ] 18 个用户故事的 AC 全部通过手动测试
- [ ] 应用能在 Android 8.0+ 真机连续使用一周不出问题
- [ ] 卸载重装后数据完全清除（无残留）
- [ ] PRD §1.3 的成功指标 7 日留存无法在单人项目验证，其他指标能演示

---

> **下一步**：开始执行阶段 0，在仓库根目录生成工程骨架。生成完后用户用 Android Studio 直接打开 **仓库根目录**（识别 `settings.gradle.kts`）→ Gradle sync → 编译运行。
