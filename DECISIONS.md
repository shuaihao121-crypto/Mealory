# Mealory 技术决策记录

最后更新：2026-10-27

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

## D-015 Room 使用 KSP 并提交 schema 历史

- 决策：Room 2.8.5 通过 KSP 2.3.12 生成代码，`app/schemas` 纳入 Git。
- 原因：AGP 9.4 使用内置 Kotlin；KSP 2.3 可直接兼容该构建模型。提交 schema 可以在后续版本自动验证数据库迁移。

## D-016 coverPhotoId 由 Repository 维护一致性

- 决策：`Meal.coverPhotoId` 不声明到 `MealPhoto` 的数据库外键，而由事务型 Repository 校验它必须指向同一 Meal 的照片。
- 原因：Meal 与 MealPhoto 已存在反向外键，直接增加循环外键会让初始插入与删除顺序复杂化；Repository 约束保留一致性并方便 Phase 3 的原子照片保存。

## D-017 照片采用 staging + 补偿清理

- 决策：照片先处理到私有 staging，全部成功后移动到正式目录，再在 Room 事务中创建 Meal；任何处理、移动或数据库错误都会删除本次 staging/正式文件。
- 原因：文件系统与 SQLite 无法共享同一个事务，先验证照片并在失败时补偿清理，可以避免半条 Meal 或长期孤儿文件。

## D-018 Coil 固定 3.3.0 直到 Kotlin 基线升级

- 决策：当前使用 Coil 3.3.0；暂不升级 3.4+。
- 原因：Coil 3.4+ 使用比 AGP 9.4 内置 Kotlin 2.2.10 更高的 Kotlin 元数据版本，直接升级会导致编译器不兼容。升级 AGP/Kotlin 后再同步更新。

## D-019 首页从首版使用 Room PagingSource

- 决策：首页时间线使用 Paging 3，页大小 30、预取距离 10、关闭占位符；排序固定为 `dateEpochDay DESC, minuteOfDay DESC, createdAt DESC`。
- 原因：这是长期累积型产品，首版就必须避免把多年、数万条 Meal 一次读入内存；稳定排序同时保证同一分钟记录的顺序可预测。

## D-020 V1 搜索使用转义后的 Room LIKE

- 决策：饭名、备注、地点和人物通过一个参数化 Room 查询联合搜索；用户输入的反斜杠、`%` 与 `_` 先转义，不把输入解释为通配符。
- 原因：V1 数据规模不需要 FTS；字面量搜索更符合用户预期，也避免特殊字符意外匹配全部记录。后续只有在真实性能数据表明必要时才迁移到 FTS。

## D-021 日历使用范围投影而非完整关系列表

- 决策：月/年日历查询只返回 `CalendarMealPreview`，通过关联子查询优先取得封面路径、没有封面时回退到排序第一张照片；只有进入某一天时才加载完整 `MealWithDetails`。
- 原因：日历格只需要日期、餐次和代表照片。按月或按年加载人物、地点和全部照片会造成无意义的 I/O，也不利于支持十几年历史数据。

