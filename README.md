# 自我档案与人物图鉴

一款面向个人用户的 Android 应用，用温暖手账风格记录自我与身边重要的人。纯本地存储、隐私优先。

> **状态**：MVP + 发布增强已完成（阶段 0-7 全量 + POST_LAUNCH_ROADMAP Phase 1-3 + 数据安全/新功能一批）
> **平台**：Android 8.0+ (minSdk 26 / targetSdk 34)
> **技术栈**：Kotlin + Jetpack Compose + Material 3 + Hilt + Room + DataStore + WorkManager

## 核心功能

- **自我档案**：14 个模块（个人背景 / 身体数据 / 尺码 / 喜恶 / 性格 / 理想型 / 憧憬 / 愿望清单 / 人生轨迹 / 随想笔记 / 照片墙 / 纪念日…），模块可排序，深链滚动
- **人物图鉴**：2 列卡片、按姓名/城市/MBTI/标签搜索、关系 + 亲密度 + 自定义标签三重筛选、4 种排序
- **关系图谱**：三层轨道星形 Canvas 图，点节点进详情
- **提醒中心**：生日 / 纪念日 / 自定义提醒，支持**农历**（每年自动换算公历调度），WorkManager 本地通知
- **数据安全**：zip 备份导出（含照片本体）到自选位置、备份导入恢复（覆盖前自动留安全备份）、回收站（30 天保留，可恢复）
- **洞察**：关系分布环形图 + 本月重要日子日历条；桌面「近期提醒」小组件
- **其他**：全局搜索、深色模式（300ms 过渡）、字体缩放 4 档、生物识别应用锁（失败降级 + 锁定倒计时）

## 文档

- [PRD](./docs/PRD.md) — 完整产品需求（18 个用户故事 + 数据模型 + 视觉规范）
- [PLAN](./docs/PLAN.md) — 详细开发计划（7 个阶段、Gradle 依赖、目录结构）
- [ROADMAP](./docs/POST_LAUNCH_ROADMAP.md) — MVP 后三阶段路线图（已全部完成）

## 怎么跑起来

> **重要**：用 Android Studio 直接打开 **本仓库根目录**（不是 `app/`），Trust Gradle Project，等首次 Sync（5-10 分钟）。

命令行构建（Windows Git Bash）：

```bash
export JAVA_HOME="D:/Program Files/Android/Android Studio/jbr"   # 指向 AS 自带 JBR（JDK 17）
./gradlew :app:assembleDebug        # Debug 包 → app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest    # 单元测试（JUnit 5，import 用 org.junit.jupiter.api.Test）
./gradlew :app:lintDebug            # Lint 报告 → app/build/reports/
```

**踩坑预警**：

- **JDK 17** — 命令行没有 JDK 时用 Android Studio 自带 JBR（路径见上）
- **JUnit 5** — 测试 import 必须 `org.junit.jupiter.api.Test`；混用 JUnit 4 会静默 skip（build 通过但 test-results 没有 xml）
- gradle wrapper 三件套（`gradlew` / `gradlew.bat` / `gradle-wrapper.jar`）已入库，无需手动生成

## 打 Release 正式包

签名从环境变量读取（keystore 不入库，`.gitignore` 已挡 `*.jks`）：

```bash
export SELF_RELEASE_STORE_FILE=/path/to/your.jks
export SELF_RELEASE_STORE_PASSWORD=****
export SELF_RELEASE_KEY_ALIAS=your-alias
export SELF_RELEASE_KEY_PASSWORD=****
./gradlew :app:assembleRelease      # → app/build/outputs/apk/release/app-release.apk
```

未配置环境变量时也能构建，产出 `-unsigned.apk`。生成 keystore：

```bash
keytool -genkeypair -v -keystore your.jks -alias your-alias \
  -keyalg RSA -keysize 2048 -validity 10000
```

## 国内依赖加速

`settings.gradle.kts` 已配阿里云镜像（`maven.aliyun.com`），AGP / Kotlin / Compose / Hilt 全部走镜像，下载速度比直连官方源快 5-10 倍。

`gradle/wrapper/gradle-wrapper.properties` 也已切到国内镜像，第一次 sync 时下载 gradle 二进制包从几十秒压到几秒。

**如果想让所有项目都用镜像**（不仅限本项目），可以在用户目录加一个全局 init 脚本，一次配置永久生效：

```kotlin
// ~/.gradle/init.d/mirrors.init.gradle.kts
allprojects {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
    }
}
settingsEvaluated {
    pluginManagement {
        repositories {
            maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
        }
    }
}
```

要切回官方源时，把 `settings.gradle.kts` 里的阿里云块删掉就行。

## 目录结构

```
self/
├── app/                                  # Android app 模块
│   ├── src/main/java/com/self/profile/
│   │   ├── data/                         # Room / DataStore / Repository / 备份 / 种子
│   │   ├── di/                           # Hilt Module
│   │   ├── domain/                       # 领域模型
│   │   ├── ui/                           # Compose（home/self/people/map/search/settings/widget 相关）
│   │   ├── widget/                       # 桌面小组件
│   │   ├── work/                         # WorkManager 提醒调度
│   │   └── util/                         # 农历 / 提醒计算 / 照片存储
│   └── src/test/                         # JUnit 5 单元测试
├── gradle/libs.versions.toml             # 所有第三方版本
├── docs/                                 # PRD / PLAN / ROADMAP
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```
