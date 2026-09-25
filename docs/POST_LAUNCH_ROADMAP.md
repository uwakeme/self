# 自我档案与人物图鉴 — 后期阶段路线图

> **状态**：已完成（Phase 1-3 全部落地并提交；延后项见 §5，以 git log 为准）  
> **起算时间**：2026-08-10  
> **基于**：PRD v1.0.0 + 已完成 Stage 0-7（17 个 commit on `main`）  
> **目标**：补齐 PRD 验收标准里缺失的细节，把 app 推到 v1.0 release-ready

---

## 0. 决策记录（已对齐）

| 决策点 | 选择 | 备注 |
|---|---|---|
| 阶段粒度 | 3 个大阶段，每个 1-2 天 | Phase 1 = 快速可见改进，Phase 2 = 数据/安全，Phase 3 = 重构+发布准备 |
| 数据管理 | 只做「清除」，不做「导入」 | 导出功能已就绪；导入的 UUID 重映射逻辑复杂，先不做 |
| 应用锁 UX | 都做（5s 阈值 + 失败重试） | PRD 明确写了 |
| 拖拽排序 | **不做** | 已用上下箭头按钮等价替代，PRD 写的是拖拽，功能上 OK |
| 单元测试 / CI / release 签名 | **不做**（本路线图） | 单独走 devops 路线，不在 v1.0 范围 |

---

## 1. 总览

| Phase | 主题 | 估时 | 关键产出 |
|---|---|---|---|
| **1** | P0 快速修复（用户能立刻看到） | 0.5-1 天 | Toast、scrollTo、连线中点圆点、sticky header |
| **2** | 数据所有权 + 应用锁 UX | 1-1.5 天 | 数据清除、AppLock 5s 阈值、失败重试降级 |
| **3** | 重构 + 发布准备 | 1-1.5 天 | 提醒中心去重、深色模式过渡、ProGuard 验证、release APK |

每个 Phase 完成后独立可发版（feature flag 思想不必要——都是非破坏性改进）。

---

## 2. Phase 1 — P0 快速修复

**目标**：用户能立刻感知到的 4 个小改进。每个改动小、孤立、低风险。

### 2.1 添加人物 Toast 反馈（AC-009-4）

**问题**：`AddPersonScreen` 保存成功后只 `onSaved()` 触发 `popBackStack`，没有"已添加"的视觉确认。

**改法**：
- `AddPersonScreen` 用 `SnackbarHostState`，保存成功后 `showSnackbar("已添加「${name}」")`
- 不用 Toast（Toast 难控制时序，且和 Material 3 风格不搭）

**文件**：
- `app/src/main/java/com/self/profile/ui/people/AddPersonScreen.kt`

**验收**：填完姓名 → 保存 → 回列表 → 看到底部 Snackbar 弹出 2 秒后消失

---

### 2.2 快捷操作点击后 scrollTo 对应模块（AC-002-3、AC-002-5）

**问题**：`QuickActionRow` 的"写笔记"和"纪念日"都只跳到 `Self.path`，没有"滚到对应 section"。

**改法**：
- AppNav 的 `composable(Route.Self.path) { ... }` 接收一个 SavedStateHandle 标志位（或 nav arg）`scrollTo: String?`，值为 `NOTES` / `ANNIVERSARIES` / `null`
- HomeScreen 的 `onWriteNote` / `onAnniversary` 跳到 `Route.Self.build("notes")` / `Route.Self.build("anniversaries")`
- SelfDetailScreen 接收 `scrollTo` 参数，`LaunchedEffect(scrollTo)` 调 `lazyListState.animateScrollToItem(itemIndex)`
- 映射表：`"notes" → 8, "anniversaries" → 11`（按当前 defaultOrder）

**文件**：
- `app/src/main/java/com/self/profile/ui/navigation/Route.kt`（Self 路由加可选参数）
- `app/src/main/java/com/self/profile/ui/navigation/AppNav.kt`（传 scrollTo arg）
- `app/src/main/java/com/self/profile/ui/home/HomeScreen.kt`（QuickActionRow 调用改）
- `app/src/main/java/com/self/profile/ui/self/SelfDetailScreen.kt`（接 scrollTo + scrollToItem）
- `app/src/main/java/com/self/profile/ui/components/QuickActionRow.kt`（改回调签名）

**验收**：
- 首页点"写笔记" → 跳到档案页 → **自动滚到"随想笔记"section 顶部**
- 首页点"纪念日" → 跳到档案页 → **自动滚到"纪念日"section 顶部**
- 滚动有 300ms 动画

---

### 2.3 关系图谱连线中点装饰圆点（AC-011-8）

**问题**：`RelationshipMapScreen` 的连线只有线条，PRD 写"连线中点显示装饰圆点"。

**改法**：
- 在 `drawNodesAndConnections` 的连线循环里加一句：
  ```kotlin
  val mid = Offset((cx + pos.x) / 2f, (cy + pos.y) / 2f)
  drawCircle(color = color, radius = 3f, center = mid)
  ```
- 用和线同色，半径 3px

**文件**：
- `app/src/main/java/com/self/profile/ui/map/RelationshipMapScreen.kt`

**验收**：图谱每条连线中点出现一个小圆点，和线同色

---

### 2.4 SelfDetail 身份头部 sticky 固定（AC-006-17）

**问题**：长档案时身份头部（头像+姓名+MBTI+基本数据行）滚出屏幕就没了，要看名字得划回顶。

**改法**：
- 用 Material 3 的 `TopAppBarDefaults.pinnedScrollBehavior()`
- 身份头部包成 `TopAppBar`（title slot 自定义），外面套 `Scaffold` 配合 nestedScroll
- LazyColumn 传 `Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)`，TopAppBar 设 `scrollBehavior = scrollBehavior`
- 滚动时身份头部会固定在屏幕顶部，列表从其下方滚动
- **PersonDetailScreen 同步做**（同一组件复用）

**文件**：
- `app/src/main/java/com/self/profile/ui/self/SelfDetailScreen.kt`（外层套 Scaffold）
- `app/src/main/java/com/self/profile/ui/people/PersonDetailScreen.kt`（同步改）

**验收**：从档案顶部向下滚，身份头部固定在屏幕顶不下滑；反向滚回原位

---

## 3. Phase 2 — 数据所有权 + 应用锁 UX

**目标**：让"用户掌控自己的数据"和"应用锁安全性"两个核心承诺都做扎实。

### 3.1 数据清除（PRD AC-014-3 部分）

**问题**：当前"数据管理"行只触发导出，没有"清除全部数据"功能。

**改法**：
- 设置页"数据管理"行点击弹 `BottomSheet`，含两个选项：
  - 「导出数据」（调 `viewModel.exportData()`）
  - 「清除全部数据」（红字，点击弹二次确认 AlertDialog）
- 二次确认文案："确定要清除所有数据吗？此操作不可撤销，建议先导出备份。"
- 清除实现：`DataExporter` 加 `clearAllData()` 方法：
  ```kotlin
  fun clearAllData() = runBlocking {
      personDao.deleteAll()
      reminderDao.deleteAll()
      appSettings.clear()  // 重置 SettingsDataStore
  }
  ```
- 清除后显示 Snackbar "已清除 X 个人物和 Y 条提醒"

**文件**：
- `app/src/main/java/com/self/profile/data/backup/DataExporter.kt`（加 `clearAllData()`）
- `app/src/main/java/com/self/profile/ui/settings/SettingsScreen.kt`（加 BottomSheet + 二次确认）
- `app/src/main/java/com/self/profile/ui/settings/SettingsViewModel.kt`（加 `clearAllData()` 方法 + ClearState）

**验收**：
- 设置页 → 数据管理 → 弹出 BottomSheet
- 选"清除全部数据" → 二次确认 → 确认 → 列表清空、设置重置
- 重新打开 app，所有人物/提醒/自定义设置都没了，回到首次安装状态

---

### 3.2 应用锁 5 秒阈值（AC-018-6）

**问题**：当前 `AppLockManager.onAppBackgrounded()` 立刻设 `isLocked = true`，切回前台就锁屏。看个通知回来也要解锁，太重。

**改法**：
- `onAppBackgrounded()` 记 `backgroundedAt = System.currentTimeMillis()`
- `onAppForegrounded()` 检查：`if (enabled && now - backgroundedAt > 5000L) isLocked = true`
- 阈值常量 `BACKGROUND_LOCK_THRESHOLD_MS = 5_000L`

**文件**：
- `app/src/main/java/com/self/profile/ui/settings/AppLockManager.kt`

**验收**：开启应用锁 → 按 Home 出去 → 1 秒内回来 → **不锁屏**；出去等 6 秒回来 → 锁屏

---

### 3.3 应用锁失败重试降级（AC-018-5）

**问题**：当前一次生物识别失败就 dismiss 对话框，没降级也没重试。

**改法**：
- `AppLockManager` 加状态机：
  - `BiometricAttempt(biometricFailCount: Int, canFallbackToDeviceCredential: Boolean)`
  - 失败 1-2 次：重试生物识别（弹同一个 Prompt）
  - 失败 3 次：自动切到 `BIOMETRIC_STRONG | DEVICE_CREDENTIAL`（API 30+）或 `BIOMETRIC_WEAK` + 提示用设备 PIN（API 26-29）
  - 设备密码错误 1-4 次：重试
  - 设备密码错误 5 次：锁定 30 秒，倒计时显示在 LockScreen 上（"请 25 秒后再试"）
- `LockScreen` 显示当前尝试次数 + 倒计时（被锁时）

**文件**：
- `app/src/main/java/com/self/profile/ui/settings/AppLockManager.kt`（状态机）
- `app/src/main/java/com/self/profile/ui/settings/LockScreen.kt`（UI 显示尝试次数/倒计时）

**验收**：
- 故意输错生物识别 3 次 → 自动切到设备 PIN 输入
- 故意输错 PIN 5 次 → 屏幕显示"请 30 秒后再试"+倒计时
- 倒计时归零后允许再试

---

## 4. Phase 3 — 重构 + 发布准备

**目标**：清理技术债，确保 release 构建跑得通。

### 4.1 提醒中心去重

**问题**：现在有两套"提醒中心" UI：
- `SettingsScreen` 里嵌了一整块（统计 + 即将到来 + 全部提醒 + 快捷设置）
- `ReminderCenterScreen` 是独立全屏页（5 Tab 筛选 + FAB 添加）
- 首页"快捷操作-提醒"和"近期提醒-查看全部"都跳到 `ReminderCenterScreen`

**改法**（推荐 A：合并到设置页）：
- `SettingsScreen` 保留提醒中心所有内容（统计 + 即将到来 + 全部提醒 + 添加）
- **删除** `ReminderCenterScreen` 和 `Route.ReminderCenter`
- 首页的"提醒"快捷操作和"查看全部 >" 都跳到 `Settings.path`（`onNavigateToSettings` 已存在）
- 验证"提醒中心"就是设置页上半部分这个心智模型

**或者**推荐 B：拆分为独立页
- `SettingsScreen` 只留"快捷设置 / 应用设置 / 关于"配置类内容
- 把"提醒中心"整块挪到 `ReminderCenterScreen`（已经独立）
- 改 tab 路由让"提醒"和"近期提醒-查看全部"都跳到 `ReminderCenter`
- 改动量大但是"配置"和"内容"分得清

**先定一个**。我推荐 A，理由：当前代码里 `SettingsScreen` 已经有完整提醒中心 UI 而且能用，删 `ReminderCenterScreen` 改 AppNav 路由省事。

**文件**：
- `app/src/main/java/com/self/profile/ui/reminder/ReminderCenterScreen.kt`（删除）
- `app/src/main/java/com/self/profile/ui/reminder/ReminderEditorDialog.kt`（保留，被 SettingsScreen 复用）
- `app/src/main/java/com/self/profile/ui/reminder/ReminderCenterViewModel.kt`（删除或合并到 SettingsViewModel）
- `app/src/main/java/com/self/profile/ui/navigation/AppNav.kt`（移除 `Route.ReminderCenter` 注册）
- `app/src/main/java/com/self/profile/ui/navigation/Route.kt`（移除 `ReminderCenter`）

**验收**：只有一处提醒中心 UI（在设置页），两个跳转入口都正确指向

---

### 4.2 深色模式 300ms 过渡动画（AC-015-2）

**问题**：当前 `SelfTheme` 直接换 `colorScheme`，切换是硬切。

**改法**：
- 包一层 `animateColorAsState`：
  ```kotlin
  val animatedBg by animateColorAsState(
      targetValue = colorScheme.background,
      animationSpec = tween(300),
      label = "theme-bg"
  )
  ```
- 关键色都做动画（background, surface, onSurface, primary, onBackground）
- 或者用 Material 3 的 `MotionScheme`（如果 Material 3 1.3+ 支持），从根上做过渡

**文件**：
- `app/src/main/java/com/self/profile/ui/theme/Theme.kt`

**验收**：设置页切深色/浅色 → 整个 app 在 300ms 内平滑过渡，没有硬切

---

### 4.3 理想型画像 / 性格分析 升 bodyLarge（AC-006-8/9）

**问题**：这两段是用户写的长文字描述，目前用 `bodyMedium` (13sp)，长段落读着偏小。

**改法**：
- `TextBlock` Composable 加 `bodyLarge: Boolean = false` 参数
- `PersonalitySection` / `IdealTypeSection` 调用时传 `bodyLarge = true`
- `bodyLarge` 是 15sp，段落感更强

**文件**：
- `app/src/main/java/com/self/profile/ui/self/SelfDetailScreen.kt`
- `app/src/main/java/com/self/profile/ui/people/PersonDetailScreen.kt`

**验收**：长描述（如 50+ 字）看起来比之前更可读，标题列对齐不变

---

### 4.4 ProGuard 验证 + Release 构建

**问题**：当前只跑过 debug，release 没验证过。R8 可能在序列化 / Hilt / Compose 上炸。

**改法**：
- 检查现有 `proguard-rules.pro`（已经有 kotlinx-serialization + Hilt + Room + Compose）
- 跑 `./gradlew assembleRelease`（或 Android Studio → Build → Generate Signed APK）
- 如果有 R8 错误，按报错补 `-keep` 规则
- 重点关注：`kotlinx.serialization` 内部类、`androidx.compose.material.icons.filled.*`、`net.zetetic.*`（如果以后加 SQLCipher）、`com.self.profile.data.local.entity.*`
- 验证 release APK 能正常装、启动、加人物、加提醒、设应用锁全跑通

**文件**：
- `app/proguard-rules.pro`（按需补规则）
- `app/build.gradle.kts`（如果需要显式 release signing config，先用 debug keystore 凑合跑通）

**验收**：`./gradlew assembleRelease` 成功，生成的 `app-release.apk` 装机后所有功能正常

---

## 5. 不在本路线图（明确推迟）

> 下表为当前实际状态：原推迟项中数据导入、桌面 widget、单元测试、Release 签名均已在 Phase 1-3 落地，从表中移出。

| 项 | 状态 / 推迟理由 |
|---|---|
| 拖拽排序（reorderable 库） | 现有排序交互功能等价，PRD 要求是 nice-to-have |
| CI / GitHub Actions | 单独 devops 路线 |
| i18n（多语言） | PRD 没要求，v1 只做中文（预留 values-en） |
| UI 文案外化 strings.xml | 推迟：v1 只做中文，外化工作量大 |
| 无障碍（contentDescription 补全） | 推迟 |
| 数据加密 | 推迟：本地个人数据场景 |
| 备份到云端 | PRD 明确说"非目标" |
| 桌面端 / Pad 适配 | minSdk 26 移动端优先 |

---

## 6. 验收总则

每个 Phase 完成后我会：
1. 跑 `git diff` 给你看改了什么
2. 列出受影响的文件
3. 给出手动验证步骤（用户跑一遍 `make project` + 手机装一下）
4. commit + push，message 用单行英文（PowerShell git 兼容）

---

## 7. 下一步

按你确认的粒度，下一步开 **Phase 1**：

```
Phase 1.1: Toast on AddPerson
Phase 1.2: QuickAction scrollTo
Phase 1.3: Map midpoint dots
Phase 1.4: Sticky identity header
```

四个独立小改，串行做完一次 `make project` 验证后整体 commit。

需要我现在就开始 Phase 1 吗？还是想先调整某个 Phase 的范围？
