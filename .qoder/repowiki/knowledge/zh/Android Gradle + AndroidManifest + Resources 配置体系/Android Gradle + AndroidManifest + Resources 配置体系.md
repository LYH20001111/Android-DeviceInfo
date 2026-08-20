---
kind: configuration_system
name: Android Gradle + AndroidManifest + Resources 配置体系
category: configuration_system
scope:
    - '**'
source_files:
    - settings.gradle.kts
    - gradle/libs.versions.toml
    - gradle.properties
    - app/build.gradle.kts
    - app/src/main/AndroidManifest.xml
    - app/src/main/res/values/strings.xml
    - app/src/main/res/values/themes.xml
    - app/src/main/res/values/colors.xml
    - app/src/main/res/xml/backup_rules.xml
    - app/src/main/res/xml/data_extraction_rules.xml
    - app/proguard-rules.pro
---

## 1. 使用的系统/方法

本仓库是一个基于 Gradle Kotlin DSL 的 Android 单模块工程，采用 Android 官方推荐的多层配置方式：
- **Gradle 构建期配置**：通过 `settings.gradle.kts`、根 `build.gradle.kts`、`app/build.gradle.kts`、`gradle.properties` 以及 `gradle/libs.versions.toml`（Version Catalog）集中管理插件版本、依赖版本、仓库源和 JVM 参数。
- **Android 运行时配置**：通过 `AndroidManifest.xml` 声明应用元信息（applicationId、activity、权限、备份规则等），通过 `res/values/*.xml`（strings、themes、colors）提供可替换的字符串与主题资源。
- **构建变体输出命名**：在 `app/build.gradle.kts` 中通过 `androidComponents.onVariants` 钩子动态生成包含版本号、构建类型和时间戳的 APK 文件名。

没有发现任何自定义的运行时配置文件加载逻辑（如 JSON/YAML/properties 解析器）、环境变量注入或 Feature Flag 框架；应用本身仅读取系统设备信息并展示，不主动加载外部配置。

## 2. 关键文件

- `settings.gradle.kts`：仓库级仓库源、插件管理、`rootProject.name = "DeviceInformation"`、`include(":app")`，并通过 `dependencyResolutionManagement.repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)` 强制所有模块统一使用顶层仓库源。
- `gradle/libs.versions.toml`：Version Catalog，集中声明 AGP、junit、espresso、appcompat、material 等版本及库别名，供各 build script 通过 `libs.xxx` 引用。
- `gradle.properties`：全局 Gradle 进程参数（`org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8`），注释说明 IDE 配置会覆盖此文件。
- `app/build.gradle.kts`：模块级构建脚本，定义 `namespace`、`compileSdk`、`defaultConfig`（applicationId/minSdk/targetSdk/versionCode/versionName）、`buildTypes.release`（混淆）、`compileOptions`（Java 11），并在 `androidComponents.onVariants` 中重写 APK 输出文件名。
- `app/src/main/AndroidManifest.xml`：声明 application 属性、MainActivity 入口、backup/dataExtraction rules 引用。
- `app/src/main/res/values/strings.xml`、`themes.xml`、`colors.xml`：应用名称、主题、颜色等运行时资源。
- `app/proguard-rules.pro`：release 构建时应用的 ProGuard/R8 规则。
- `app/src/main/res/xml/backup_rules.xml`、`data_extraction_rules.xml`：Android 12+ 数据备份与提取策略。

## 3. 架构与约定

- **分层清晰**：Gradle 构建配置与 Android 运行时配置严格分离。构建期变量（versionCode、versionName、minSdk 等）集中在 `app/build.gradle.kts` 的 `defaultConfig`；运行时可见的配置（应用名、图标、主题）放在 `res/` 下。
- **版本集中化**：所有第三方依赖版本通过 `gradle/libs.versions.toml` 的 `[versions]` / `[libraries]` / `[plugins]` 三段式维护，模块内只引用 `libs.*` 别名，避免硬编码版本号。
- **仓库源收敛**：`settings.gradle.kts` 中 `repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)` 禁止子模块自行声明仓库，确保依赖来源可控。
- **构建产物命名约定**：APK 文件名格式为 `{flavor|projectName}-{versionCode}-{versionName}-{buildType}-{yyyyMMddHHmm}.apk`，由 `androidComponents.onVariants` 在构建期拼接生成。
- **资源多密度支持**：图标等资源按 `mipmap-hdpi/mdpi/xhdpi/xxhdpi/xxxhdpi` 及 `mipmap-anydpi-v26` 目录组织，适配不同屏幕密度。
- **构建类型**：当前仅定义了 `release` 构建类型（关闭 minify，启用 proguard-rules.pro），未定义 debug 变体以外的 flavor。

## 4. 约定与约束

- **Gradle 进程参数必须通过 `gradle.properties` 设置**：该文件顶部注释明确说明 “IDE (e.g. Android Studio) users: Gradle settings configured through the IDE will override any settings specified in this file”，因此本地调试时应优先在 IDE 中配置而非直接修改此文件。
- **禁止子模块声明独立仓库源**：`RepositoriesMode.FAIL_ON_PROJECT_REPOS` 会在子模块添加仓库时抛出异常，强制所有依赖从 `settings.gradle.kts` 中声明的 google/mavenCentral/gradlePluginPortal 获取。
- **应用标识统一**：`namespace`、`applicationId`、`rootProject.name` 均指向 `com.newland.deviceinformation` / `DeviceInformation`，保持包名与项目名一致。
- **Java 兼容性锁定为 11**：`compileOptions.sourceCompatibility` 与 `targetCompatibility` 固定为 `JavaVersion.VERSION_11`，新增代码需遵循该目标版本。
- **Release 构建默认关闭代码压缩**：`isMinifyEnabled = false`，如需开启需在 release 块中显式调整。
- **无运行时配置加载机制**：工程中未发现任何自定义配置文件解析、环境变量读取或 feature flag 开关；所有行为差异通过构建变体（buildType/flavor）和资源替代实现。
- **备份与数据提取策略必须声明**：`AndroidManifest.xml` 引用了 `@xml/backup_rules` 与 `@xml/data_extraction_rules`，这两个 XML 文件必须存在，否则 manifest 校验失败。