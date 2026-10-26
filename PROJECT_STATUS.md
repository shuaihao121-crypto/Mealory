# Mealory 项目状态

最后更新：2026-10-26  
当前阶段：Phase 3 — 记录一顿饭（准备实现）

## 已完成

### Phase 0 环境

- 检查 Git、winget、磁盘、Android 开发工具与设备状态。
- 确认 Git 2.55.0、winget 1.29.380 可用。
- 确认 D 盘可用约 111.5 GB；采用当前绑定工作区作为项目根目录。
- 安装 Android Studio 2026.2.1.8 稳定版。
- 使用 Android Studio 内置 JBR OpenJDK 25.0.3，不另装 Java。
- 安装 Android SDK Command-line Tools 23.0、Platform 37.0、Build Tools 37.0.0、Platform Tools 37.0.1、Sources 37.0、Emulator 37.2.12。
- 安装并校验 API 37 AOSP ATD x86_64 系统镜像，创建 `Mealory_API_37` 模拟器。
- 模拟器成功启动为 Android 17 / API 37，adb 状态正常。
- adb 可执行；环境检查时未发现已连接 Android 设备。
- Git 仓库已存在，已添加 Android `.gitignore`。

### Phase 1 实现

- 创建 Kotlin/Compose/Material 3 单 Activity 工程。
- 完成自定义浅色/深色 Mealory 主题与品牌色。
- 从最终品牌板提取开发用 Logo/launcher 位图。
- 完成轻微淡入启动页。
- 完成首次进入文案与“开始记录”。
- 完成首页/日历/回忆/我的四个固定 Tab。
- 完成空首页、搜索入口、悬浮记录按钮与三入口 Bottom Sheet 骨架。
- Manifest 无 INTERNET 权限；关闭系统备份并排除 data extraction。
- 添加基础单元测试与 Compose UI 测试，覆盖首次引导、四 Tab 与记录入口 Bottom Sheet。
- 创建 README、PRD、ARCHITECTURE、DECISIONS、PROJECT_STATUS。
- Debug APK 已在 `Mealory_API_37` 安装并成功启动。
- `assembleDebug`、`testDebugUnitTest`、`lintDebug`、`connectedDebugAndroidTest` 全部通过；lint 为 0 errors / 0 warnings。

### Phase 2 数据层

- 引入 Room 2.8.4、DataStore 1.2.0、KSP 2.3.12 与 Coroutines 1.11.0。
- 建立 Meal、MealPhoto、Person、Place、MealPersonCrossRef 实体、索引、外键与 Room schema v1。
- 建立 Meal/Photo/Person/Place DAO 与事务型 `MealRepository`。
- 支持 Meal 创建、读取、编辑、软删除、恢复与永久删除；限制每顿最多 9 张照片。
- Person 删除只清关系，Place 删除自动将 Meal.placeId 置空，Meal 永久删除级联清理照片行与人物关系。
- 建立 `MealoryApplication → AppContainer → Database / Repository / SettingsRepository`。
- DataStore 已保存 onboarding、主题、照片质量、上次备份时间与版本化餐次区间。
- 首次引导完成状态已持久化；App 重启后直接进入首页。
- 自动餐次边界按产品规则实现并通过边界测试。
- 2 个 JVM 测试与 5 个设备测试全部通过；lint 为 0 issues。

## 正在进行

- Phase 3：系统相机、Photo Picker、多照片、EXIF、照片处理与记录编辑/保存流程。

## 已知问题 / 临时项

- 用户提供的是品牌展示板，不是分层透明源文件。当前图标保持既定方向并用于开发；若提供高清透明源，只需替换 `app/src/main/res/drawable-nodpi/mealory_logo.png`。
- 记录 Bottom Sheet 的三种入口目前仅为界面骨架，真实相机/Picker/无图编辑流程属于 Phase 3。
- 当前无真机连接；系统相机、Photo Picker 与 SAF 最终验收必须等待真机 USB 调试与 RSA 授权。

## 下一步

1. 建立 App 私有照片 staging/正式目录与 FileProvider 临时相机 URI。
2. 接入系统相机与 Android Photo Picker（单张/多张，最多 9 张）。
3. 实现 EXIF 时间/方向、缩放压缩、编辑页与原子保存测试。

## 阶段状态

| 阶段 | 状态 |
|---|---|
| Phase 0 环境 | 完成 |
| Phase 1 工程骨架 | 完成；已构建、lint、模拟器安装与设备测试 |
| Phase 2 数据层 | 完成；Room schema v1、Repository、DataStore 与自动测试通过 |
| Phase 3 记录流程 | 准备实现 |
| Phase 4 首页/详情/搜索 | 未开始 |
| Phase 5 日历 | 未开始 |
| Phase 6 人物/地点 | 未开始 |
| Phase 7 回忆 | 未开始 |
| Phase 8 年度回忆 | 未开始 |
| Phase 9 最近删除 | 未开始 |
| Phase 10 备份恢复 | 未开始 |
| Phase 11 设置 | 未开始 |
| Phase 12 性能/异常 | 未开始 |
| Phase 13 最终 QA | 未开始 |
