# AGENTS.md

「自我档案与人物图鉴」——个人 Android 手账 App，Kotlin + Jetpack Compose + Material 3 + Hilt + Room + DataStore。AGP 8.6.1 / Kotlin 2.0.21 / Gradle 8.7 / JDK 17，minSdk 26 / targetSdk 35，applicationId `com.self.profile`。

完整需求 `docs/PRD.md` · 阶段计划 `docs/PLAN.md` · MVP 后路线图 `docs/POST_LAUNCH_ROADMAP.md`（已完成）· 用户文档 `README.md`。

## Setup

- 打开方式：Android Studio 直接 Open **仓库根目录** `self/`（不是 `app/`），Trust Gradle Project，等首次 Sync（5-10 分钟）
- Windows 命令行用 `.\gradlew.bat`（别用裸 `gradle`；macOS/Linux 等价 `./gradlew`）；命令行无 JDK 时把 `JAVA_HOME` 指到 AS 自带 JBR（`Android Studio/jbr`）
- Build Debug APK：`.\gradlew.bat :app:assembleDebug` → `app/build/outputs/apk/debug/`
- Build Release APK：`.\gradlew.bat :app:assembleRelease`（minify + shrink + 签名开启；签名读环境变量 `SELF_RELEASE_STORE_FILE` / `SELF_RELEASE_STORE_PASSWORD` / `SELF_RELEASE_KEY_ALIAS` / `SELF_RELEASE_KEY_PASSWORD`，未配置出 unsigned 包）
- Lint：`.\gradlew.bat :app:lintDebug` → `app/build/reports/lint-results-debug.html`
- 单元测试：`.\gradlew.bat :app:testDebugUnitTest`（JUnit 5 + mockk，见 Testing 节）
- 设备测试：`.\gradlew.bat :app:connectedAndroidTest`

> **镜像**：`settings.gradle.kts` 已配阿里云 Maven 三件套（`maven.aliyun.com/repository/{public,google,gradle-plugin}`），wrapper 走 `mirrors.cloud.tencent.com/gradle/gradle-8.7-all.zip`。**别动**这些镜像配置；如要全局生效见 README「国内依赖加速」一节。

## Project layout

仓库根 = `settings.gradle.kts` 所在目录，Android Studio 打开这个根。

- `app/` — 唯一模块
  - `src/main/java/com/self/profile/` — 源码按职责分包
    - `data/` — Room 实体 / DAO / Repository / DataStore / 备份导出导入 / 种子数据
    - `di/` — Hilt Module
    - `domain/` — 领域模型（Person / Reminder / AppSettings / SelfModule）
    - `ui/` — Compose UI，按功能再分 `home/` `people/` `map/` `reminder/` `self/` `search/` `settings/` `navigation/` `theme/` `components/`（含 `dialogs/`）
    - `widget/` — 桌面小组件（RemoteViews）
    - `work/` — WorkManager Worker（Hilt 注入）
    - `util/` — 工具类（农历 / 提醒计算 / 照片存储）
  - `src/main/res/` — 资源（`drawable` `mipmap` `values` `values-night` `xml`）
  - `src/test/` — JUnit 5 单元测试（见 Testing 节）
  - `build.gradle.kts` `proguard-rules.pro`（Hilt / Room / WorkManager / kotlinx.serialization keep 规则）
- `gradle/libs.versions.toml` — 所有第三方库版本统一管理
- `docs/PRD.md` `docs/PLAN.md` `docs/POST_LAUNCH_ROADMAP.md` — 产品 / 阶段 / 路线图

> 数据库当前 version 3，开发阶段（未上线）用 `fallbackToDestructiveMigration`：schema 变更直接重建、不写 Migration（用户已确认开发期数据可清）。**上线前必须**删掉 fallback、开 `exportSchema`、补正式 Migration。

## Code style

- Kotlin 官方风格（`gradle.properties: kotlin.code.style=official`），**不要**塞自定义 `.editorconfig`
- Compose 默认 Material 3；图标统一用 `androidx.compose.material.icons.extended`
- ViewModel 用 Hilt 注入；Composable 拿 VM 走 `hiltViewModel()`
- 公共 Composable 抽到 `ui/components/`，对话框统一 `ui/components/dialogs/`
- 资源字符串走 `res/values/strings.xml`，**不**在 Composable 里写死中文
- 第三方版本号只改 `gradle/libs.versions.toml`，**不**散写在 `app/build.gradle.kts` 里
- opt-in 默认开启（`ExperimentalMaterial3Api` `ExperimentalFoundationApi` `ExperimentalLayoutApi` `ExperimentalAnimationApi`），新加 opt-in 写到 `app/build.gradle.kts:freeCompilerArgs`

## Testing

- 单测目录 `app/src/test/java/com/self/profile/` 已建，基建已配（JUnit 5 + mockk + `useJUnitPlatform()`）。新代码请补单测到这里；纯逻辑（计算 / 序列化 / 换算）优先
- 框架：JUnit 5。**注意** import 用 `org.junit.jupiter.api.Test`，不是 `org.junit.Test`（JUnit 4 会被静默 skip，build 通过但 `test-results` 找不到新 test class 的 xml）
- Mock 用 **mockk**（不要引 Mockito，跟 Hilt/Room/Compose 生态更搭）
- 已知坑（项目里踩过，跨阶段适用）：
  - `Iterable.maxOf` / `first()` / `reduce()` 在空集合上炸，新装 App 无数据时 Stats tab 会 crash → 用 `*OrNull` 或空态文案
  - Hilt 注入早于 `App.onCreate` 的 super 之后：被同步触发的 `@Provides` 要自己负责 cache miss bootstrap
  - `synchronized` 块内不能调 suspend，要用 `kotlinx.coroutines.sync.Mutex` + `withLock`

## PR & commit

- 主分支 `main`（remote HEAD，仓库只有这一个分支）；个人项目轻流程
- Commit 偏好 conventional commits 风格（`feat:` `fix:` `refactor:` `docs:` `style:` `test:` `chore:`），不强求
- 推送前本地跑一次 `.\gradlew.bat :app:assembleDebug` + `:app:testDebugUnitTest` 确认编过测过；CI 已配（`.github/workflows/android.yml`，GitHub Actions 跑 assembleDebug + testDebugUnitTest + lintDebug）

## Security

- **不**提交 keystore / `*.jks` / `google-services.json` / `local.properties` / `.env` / 含密钥的代码
- `.gitignore` 已覆盖 `local.properties` `*.jks` `*.keystore` `.archive/`，新加忽略项请保持文件极简
- Release 签名：已配 `signingConfigs.release`，密钥从环境变量读（见 Setup 节）；正式分发前用自己的 keystore 出包
