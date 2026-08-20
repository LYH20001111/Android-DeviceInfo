---
kind: build_system
name: Gradle Kotlin DSL + Version Catalog 构建系统
category: build_system
scope:
    - '**'
source_files:
    - settings.gradle.kts
    - gradle/libs.versions.toml
    - build.gradle.kts
    - app/build.gradle.kts
    - gradle.properties
    - app/proguard-rules.pro
---

## 1. 使用的系统与工具

本项目采用 **Android Gradle Plugin (AGP) 9.1.1**，使用 **Gradle Kotlin DSL**（`build.gradle.kts`、`settings.gradle.kts`）作为构建脚本语言。依赖与插件版本通过 **Version Catalog**（`gradle/libs.versions.toml`）集中管理，并通过 `alias(libs.plugins.android.application)` 在模块中引用。

- 根工程仅声明顶层插件别名并应用 false，实际 Android 插件由 `app` 模块引入。
- 使用 `org.gradle.toolchains.foojay-resolver-convention` v1.0.0 自动解析 JDK Toolchain。
- 仓库源统一在 `settings.gradle.kts` 的 `dependencyResolutionManagement` 中声明为 `google()`、`mavenCentral()`，并启用 `RepositoriesMode.FAIL_ON_PROJECT_REPOS`，禁止子项目自行添加仓库。

## 2. 关键文件

| 文件 | 作用 |
|---|---|
| `settings.gradle.kts` | 定义插件仓库、依赖仓库、Toolchain 解析器、项目名称 `DeviceInformation`、包含 `:app` 单模块 |
| `gradle/libs.versions.toml` | 集中声明 AGP、AppCompat、Material、JUnit、Espresso 等版本及插件别名 |
| `build.gradle.kts`（根） | 仅引入 `android-application` 插件别名 |
| `app/build.gradle.kts` | 模块级构建配置：命名空间、compileSdk/targetSdk/minSdk、版本信息、构建类型、输出 APK 重命名逻辑、依赖声明 |
| `gradle.properties` | 全局 JVM 参数 `-Xmx2048m -Dfile.encoding=UTF-8` |
| `app/proguard-rules.pro` | Release 构建的混淆规则 |
| `gradlew` / `gradlew.bat` | Gradle Wrapper |

## 3. 架构与约定

### 3.1 单模块结构
工程为单模块 Android 应用，`rootProject.name = "DeviceInformation"`，仅 `include(":app")`。所有业务代码、资源、清单均位于 `app/src/main`。

### 3.2 版本与 SDK 策略
- `compileSdk` 通过自定义 `release(36){ minorApiLevel = 1 }` 函数设置，targetSdk 与 minSdk 分别为 36 和 24。
- Java 编译目标为 `VERSION_11`。
- 版本号：`versionCode = 2`，`versionName = "2.0"`，位于 `defaultConfig`。

### 3.3 构建变体与产物命名
通过 `androidComponents.onVariants` 钩子在每个 Variant 上动态生成 APK 文件名，格式为：
```
{flavor或项目名}-{versionCode}-{versionName}-{buildType}-{yyyyMMddHHmm}.APK
```
例如：`DeviceInformation-2-2.0-release-202501011200.APK`。该逻辑读取 `VariantOutputImpl` 并调用 `outputFileName.set(...)` 重写输出名称。

### 3.4 构建类型
仅定义 `release` 构建类型，默认关闭混淆（`isMinifyEnabled = false`），但保留 ProGuard 配置文件路径（`proguard-android-optimize.txt` + `proguard-rules.pro`）。

### 3.5 测试依赖
- 单元测试：`junit:junit:4.13.2`
- 仪器测试：`androidx.test.ext:junit` + `espresso-core:3.7.0`

## 4. 约定与约束

- **仓库集中管控**：`dependencyResolutionManagement.repositoriesMode` 设为 `FAIL_ON_PROJECT_REPOS`，任何子模块若尝试声明自己的仓库将导致构建失败。
- **插件版本集中化**：所有插件与库版本集中在 `gradle/libs.versions.toml`，模块内通过 `libs.plugins.*` 与 `libs.*` 引用，禁止硬编码版本号。
- **JDK 自动解析**：通过 Foojay Resolver 插件自动下载/匹配所需 JDK Toolchain，无需开发者手动配置 JAVA_HOME。
- **APK 命名规范**：所有构建产物统一按 `{name}-{code}-{name}-{type}-{timestamp}.APK` 大写后缀命名，便于 CI 识别与归档。
- **无 CI/Docker/Makefile**：仓库未包含 GitHub Actions、Jenkinsfile、Dockerfile 或 Makefile；构建入口仅为标准 Gradle Wrapper 命令（`./gradlew assembleRelease` 等）。
- **ProGuard 可开关**：Release 构建默认不启用混淆，但已预留规则文件，可按需开启。