# 自我档案与人物图鉴

一款面向个人用户的 Android 应用，用温暖手账风格记录自我与身边重要的人。

> **状态**：开发中（阶段 0 / 7 完成）
> **平台**：Android 8.0+ (minSdk 26 / targetSdk 34)
> **技术栈**：Kotlin + Jetpack Compose + Material 3 + Hilt + Room + DataStore

## 文档

- [PRD](./docs/PRD.md) — 完整产品需求（18 个用户故事 + 数据模型 + 视觉规范）
- [PLAN](./docs/PLAN.md) — 详细开发计划（7 个阶段、Gradle 依赖、目录结构）
- [原型](./design/) — 6 个 HTML 页面 + 颜色字体 token

## 怎么跑起来

> **重要**：用 Android Studio 直接打开 **本仓库根目录**（也就是 `self/` 自己，不是某个子目录），AS 会识别 `settings.gradle.kts` 自动当 Android 项目加载。

1. Android Studio 打开仓库根目录 `self/`
2. 第一次会提示 "Trust Gradle Project"，点 Trust
3. 等 Gradle Sync（首次 5-10 分钟：下载 AGP / Kotlin / Compose / Hilt）
4. 顶部 Run ▶️，选 Android 8.0+ 真机或模拟器
5. 看到 5 个 Tab 占位文字就说明骨架跑通了

**踩坑预警**：

- **JDK 17** — File → Project Structure → SDK Location → Gradle JDK
- **Hub icon** — 如果编译报 `Icons.Filled.Hub` 找不到，把它换成 `Icons.Filled.AccountTree`（`ui/navigation/BottomBar.kt`）
- **Gradle wrapper jar** — AS 第一次 sync 会自动生成；如没自动生成跑一次 `gradle wrapper --gradle-version 8.7`

## 国内依赖加速

`settings.gradle.kts` 已配阿里云镜像（`maven.aliyun.com`），AGP / Kotlin / Compose / Hilt 全部走镜像，下载速度比直连官方源快 5-10 倍。

`gradle/wrapper/gradle-wrapper.properties` 也已切到阿里云 Gradle 分发镜像（`mirrors.aliyun.com/macports/distfiles/gradle`），第一次 sync 时下载 130MB 的 gradle 二进制包从几十秒压到几秒。

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

要切回官方源时，把 `settings.gradle.kts` 里的阿里云块删掉就行，或者临时设环境变量 `GRADLE_OPTS=-Dorg.gradle.internal.repository.max.tentatives=1` 跳过镜像 fallback。

## 目录结构

```
self/
├── app/                                  # Android app 模块
│   └── src/main/java/com/self/profile/...
├── gradle/                               # Gradle wrapper
│   ├── libs.versions.toml
│   └── wrapper/
├── docs/
│   ├── PRD.md
│   └── PLAN.md
├── design/                               # HTML 原型 + 颜色 token
│   ├── pages/
│   └── colors_and_type.css
├── .archive/                             # 历史归档（不参与编译）
├── build.gradle.kts                      # 根 Gradle
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

## 当前阶段

### 阶段 0：项目骨架（已完成）

- Gradle 8.7 + AGP 8.5.2 + Kotlin 2.0.20 + Compose BOM 2024.06.00 + Hilt 2.51.1
- 暖手账色板从 `design/colors_and_type.css` 翻译到 Compose `Color`
- Material 3 主题（亮 / 暗 双套）
- 5 Tab 路由 + Bottom Navigation
- 自适应应用图标
- 占位 Screen（5 个 Tab + 2 个子页面）

### 下一阶段：阶段 1

详见 [PLAN.md §4 阶段 1](./docs/PLAN.md)。Room + DataStore + 种子数据 + 5 Tab 全部静态 UI。
