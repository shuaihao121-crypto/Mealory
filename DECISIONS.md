# Mealory 技术决策记录

最后更新：2026-10-26

## D-001 使用当前 Codex 工作区作为工程根目录

- 决策：工程根目录为 `D:\Documents\ChatGPT\Mealory`，不另复制到 `D:\Projects\Mealory`。
- 原因：该目录已由桌面任务绑定并初始化为空 Git 仓库，D 盘可用空间约 111.5 GB；继续在此工作可保证会话、文件链接和后续自动化稳定。
- 影响：与需求中的优先建议路径不同，但仍位于 D 盘且满足容量要求。

## D-002 只使用 Android Studio 内置 JDK

- 决策：不安装系统 Java 或独立 Gradle；构建时 `JAVA_HOME` 指向 Android Studio `jbr`。
- 原因：避免多 JDK 冲突，符合产品要求。

## D-003 稳定 SDK 37.0 与 minSdk 26

- 决策：compileSdk/targetSdk 37，minSdk 26；使用稳定 Build Tools 37.0.0。
- 原因：截至 2026-10-25，API 37.0 为稳定平台，AGP 9.4 官方最高支持 API 37。拒绝使用 37.2 beta。

## D-004 AGP 9.4.1 + Gradle 9.6.0

- 决策：使用 AGP 9.4.1、其内置 Kotlin 2.2.10、Compose Compiler plugin 2.2.10、Gradle 9.6.0。
- 原因：AGP 9.4 官方兼容表要求 Gradle 9.6.0；使用内置 Kotlin 避免重复应用已不需要的 Android Kotlin plugin。

## D-005 单 Activity、单 module、简单容器注入

- 决策：保持一个 app module；后续由 MealoryApplication 创建 AppContainer。
- 原因：产品规模不需要 Hilt 或复杂 Clean Architecture；显式依赖便于测试和长期维护。

## D-006 当地日期/时间是业务真值

- 决策：Meal 分开保存 dateEpochDay、minuteOfDay、timeZoneId；操作审计时间另存 UTC epoch。
- 原因：跨时区后不能让历史晚餐从 19:30 变成其他时间。

## D-007 正式照片归档到 App 私有目录

- 决策：Picker/相机 URI 只作为导入源，处理后复制到私有目录，数据库只保存相对路径。
- 原因：外部 URI 权限与原图可能消失；长期保存不能依赖相册。

## D-008 不保留永久缩略图树

- 决策：正式只存处理后的原图；页面缩略图由 Coil 按请求尺寸加载并缓存。
- 原因：减少存储膨胀和清理复杂度。

## D-009 软删除 30 天

- 决策：普通删除只写 `deletedAt`；恢复清空该字段；WorkManager 与启动检查负责清理。
- 原因：降低误删风险，同时保持查询规则简单。

## D-010 备份使用开放 ZIP，不以 Room 文件为唯一格式

- 决策：JSON + JPG + manifest + SHA-256；恢复先在 staging 完整验证和构建，再原子切换。
- 原因：数据长期可理解、可迁移；恢复失败不能破坏当前数据。

## D-011 禁止系统云备份与网络权限

- 决策：Manifest 不含 INTERNET；`allowBackup=false` 并排除 data extraction/full backup 的全部域。
- 原因：落实“数据只在设备中”的承诺；由用户主动导出备份。

## D-012 Phase 1 品牌位图来源

- 决策：从用户最终品牌板提取现有 app icon 作为 Phase 1 启动页和 launcher 资源，不改变品牌方向。
- 现状：附件是完整品牌展示板而非分层原始 Logo；当前提取位图可用于开发与视觉验收。
- 后续：如获得透明高清源文件，只替换 `app/src/main/res/drawable-nodpi/mealory_logo.png`，不改 UI 代码。

## D-013 模拟器采用稳定 API 37 AOSP ATD 镜像

- 决策：Phase 0/1 使用 API 37 AOSP ATD x86_64；不等待体积 2.08 GB 且当前网络异常慢的 Google Play 镜像。
- 原因：ATD 适合自动化基础回归且体积更小。相机、Photo Picker、SAF 与备份恢复仍必须在真机完成最终验收。

## D-014 构建仓库使用校验过的国内镜像优先、官方源回退

- 决策：插件与依赖解析优先使用阿里云的 Google/Maven Central/Gradle Plugin 镜像，并保留 `google()`、`mavenCentral()`、`gradlePluginPortal()` 作为回退；Gradle 分发包可从华为云镜像取得，但必须匹配 Gradle 官方 SHA-256。
- 原因：当前网络对 GitHub 与部分 Google 大文件连接长时间无响应，官方源单独使用无法稳定完成首次构建。镜像仅改变传输路径，不改变锁定的坐标与校验值。

