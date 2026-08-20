---
kind: dependency_management
name: 基于 Gradle Version Catalog 的 Android 依赖管理
category: dependency_management
scope:
    - '**'
source_files:
    - gradle/libs.versions.toml
    - settings.gradle.kts
    - app/build.gradle.kts
    - build.gradle.kts
    - gradle.properties
---

## 1. 使用的系统与工具

本项目采用 **Gradle Kotlin DSL** 构建，并使用 Gradle 官方推荐的 **Version Catalog（版本目录）** 机制集中管理所有第三方依赖与插件版本。核心文件为 `gradle/libs.versions.toml`，通过 `[versions]`、`[libraries]`、`[plugins]` 三段式声明统一版本来源。

- 包管理器：Gradle 内置依赖解析器，仓库源包括 Google Maven (`google()`)、Maven Central (`mavenCentral()`) 以及 Gradle Plugin Portal (`gradlePluginPortal()`)。
- 无私有仓库或镜像配置，未使用 `gradle.properties` 中的代理或认证参数。
- 未启用 `dependencyVerification` 或签名校验，也未发现 `gradle.lockfile` 等锁定文件。

## 2. 关键文件

- `gradle/libs.versions.toml`：全局版本目录，集中声明 AGP、AndroidX、Material、JUnit、Espresso 等库及插件的版本号，并通过 `version.ref = "..."` 引用共享版本。
- `settings.gradle.kts`：定义 `pluginManagement.repositories` 和 `dependencyResolutionManagement.repositories`，并设置 `RepositoriesMode.FAIL_ON_PROJECT_REPOS`，强制所有模块禁止自行添加仓库源。
- `build.gradle.kts`（根）：仅通过 `alias(libs.plugins.android.application) apply false` 声明全局可用插件。
- `app/build.gradle.kts`：应用模块实际引用依赖，全部通过 `libs.xxx` 别名访问，如 `implementation(libs.appcompat)`、`testImplementation(libs.junit)`。
- `gradle.properties`：仅包含 JVM 内存参数，不包含任何依赖相关配置。

## 3. 架构与约定

- **版本集中化**：所有依赖版本号集中在 `gradle/libs.versions.toml` 的 `[versions]` 段，`[libraries]` 段通过 `group/name/version.ref` 形式引用这些版本；新增库时需在两处同步维护。
- **插件版本集中化**：AGP 插件通过 `[plugins] android-application = { id = "com.android.application", version.ref = "agp" }` 声明，并在各模块中以 `alias(libs.plugins.android.application)` 方式引入。
- **仓库源集中管控**：`settings.gradle.kts` 中 `dependencyResolutionManagement.repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)` 强制子模块不得在自身 `build.gradle.kts` 中添加 `repositories {}`，确保仓库源单一可控。
- **插件仓库隔离**：`pluginManagement.repositories` 额外包含 `gradlePluginPortal()` 并限制 `includeGroupByRegex("com\.android.*|com\.google.*|androidx.*")`，用于解析 Gradle 插件本身。
- **依赖作用域清晰**：测试依赖使用 `testImplementation` / `androidTestImplementation` 与运行时依赖 `implementation` 严格区分。
- **Java 版本约束**：通过 `compileOptions.sourceCompatibility/targetCompatibility = JavaVersion.VERSION_11` 固定编译目标。

## 4. 约定与约束

- **必须通过 Version Catalog 引用依赖**：模块内 `dependencies {}` 块仅出现 `libs.xxx` 形式的调用，未发现直接写死 group/name/version 的依赖声明。
- **禁止在模块级添加仓库源**：`FAIL_ON_PROJECT_REPOS` 会在子模块声明 `repositories {}` 时报错，强制所有仓库配置集中在 `settings.gradle.kts`。
- **版本升级需修改 `libs.versions.toml`**：升级第三方库时只需更新该文件中对应版本号，无需逐个模块查找。
- **未使用依赖锁定/缓存策略**：仓库中不存在 `gradle.lockfile`、`*.lock` 文件或自定义依赖缓存脚本，依赖解析完全由 Gradle 默认行为处理。
- **未配置私有仓库/代理**：`gradle.properties` 与 `settings.gradle.kts` 中均未出现 `systemProp.http.proxy*`、`mavenLocal()`、`maven { url=... }` 等私有源配置。
- **AGP 版本与 compileSdk/targetSdk 分离**：AGP 版本由 Version Catalog 管理（当前 `9.1.1`），而 `compileSdk`/`targetSdk` 直接在 `app/build.gradle.kts` 中硬编码为 `36`，两者不在同一位置统一管理。