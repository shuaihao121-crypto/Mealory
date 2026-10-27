# Mealory 项目状态

最后更新：2026-10-27
当前阶段：Phase 5 — 日历（准备实现）

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

- 引入 Room 2.8.5、DataStore 1.3.0、KSP 2.3.12 与 Coroutines 1.11.0。
- 建立 Meal、MealPhoto、Person、Place、MealPersonCrossRef 实体、索引、外键与 Room schema v1。
- 建立 Meal/Photo/Person/Place DAO 与事务型 `MealRepository`。
- 支持 Meal 创建、读取、编辑、软删除、恢复与永久删除；限制每顿最多 9 张照片。
- Person 删除只清关系，Place 删除自动将 Meal.placeId 置空，Meal 永久删除级联清理照片行与人物关系。
- 建立 `MealoryApplication → AppContainer → Database / Repository / SettingsRepository`。
- DataStore 已保存 onboarding、主题、照片质量、上次备份时间与版本化餐次区间。
- 首次引导完成状态已持久化；App 重启后直接进入首页。
- 自动餐次边界按产品规则实现并通过边界测试。
- 2 个 JVM 测试与 5 个设备测试全部通过；lint 为 0 issues。

### Phase 3 记录流程

- 使用 `TakePicture + FileProvider` 调用系统相机，不申请 CAMERA 权限。
- 使用 Android Photo Picker 多选照片，不申请相册读取权限；每顿最多 9 张。
- 完成无照片补记、照片删除、排序、设置封面与继续添加。
- 完成餐次、日期、时间、饭名、地点、人物和备注编辑页；文本字段均可留空。
- 读取 EXIF `DateTimeOriginal`、时区偏移与 orientation；旧照片时间可自动带入并仍由用户确认。
- 标准质量最长边约 2560px / JPEG 90%，高质量最长边约 4096px / JPEG 95%。
- 照片先写 staging，再移动到 App 私有 `pictures/meals/YYYY/MM/...`；数据库失败会补偿清理。
- 相机临时文件在保存、取消或 ViewModel 销毁时清理。
- 保存后使用“这一顿，记下了。”轻量 Snackbar 反馈。
- API 37 模拟器已实际完成系统相机拍照、Photo Picker 选择、编辑页回传和私有目录保存。
- 2 个 JVM 测试与 9 个设备测试全部通过；照片测试覆盖 EXIF 旋转、缩放、归档、有/无照片保存和 9 张上限；lint 为 0 issues。

### Phase 4 首页、详情与搜索

- 首页接入 Room Paging 3，按当地日期、时间和创建时间倒序分页加载，避免一次读取全部历史记录。
- 完成日期分组、今天/昨天标识、大照片饭卡、餐次、时间、饭名、地点与人物摘要。
- 完成饭名、备注、地点和人物的本地联合搜索；LIKE 通配符按字面量转义，软删除记录不会出现在搜索结果。
- 完成 Meal 详情路由、多照片左右浏览、封面展示与切换。
- 完成日期、时间、餐次、饭名和备注编辑；保存后详情与首页自动刷新。
- 普通删除使用二次确认并只执行软删除，删除后立即从首页时间线消失。
- Room 设备测试新增分页顺序、四类搜索、软删除过滤与同餐封面约束；Compose UI 测试覆盖详情、编辑保存和删除确认。
- API 37 模拟器已实际验证：首页即时刷新、日期分组、四类搜索、无结果状态、详情、多图滑动、切换封面、编辑保存和软删除。
- 2 个 JVM 测试与 11 个设备测试全部通过；lint 为 0 issues。

## 正在进行

- Phase 5：日历月视图、日期预览/详情和年份视图。

## 已知问题 / 临时项

- 用户提供的是品牌展示板，不是分层透明源文件。当前图标保持既定方向并用于开发；若提供高清透明源，只需替换 `app/src/main/res/drawable-nodpi/mealory_logo.png`。
- 当前无真机连接；系统相机、Photo Picker 与 SAF 最终验收必须等待真机 USB 调试与 RSA 授权。

## 下一步

1. 建立按日期和月份聚合的 Room 查询，不加载全量历史照片。
2. 实现月视图与有记录日期的代表照片。
3. 实现当天全部 Meal 预览/日期详情与年份十二个月视图。

## 阶段状态

| 阶段 | 状态 |
|---|---|
| Phase 0 环境 | 完成 |
| Phase 1 工程骨架 | 完成；已构建、lint、模拟器安装与设备测试 |
| Phase 2 数据层 | 完成；Room schema v1、Repository、DataStore 与自动测试通过 |
| Phase 3 记录流程 | 完成；相机、Picker、EXIF、私有照片归档与设备测试通过 |
| Phase 4 首页/详情/搜索 | 完成；Paging 时间线、详情编辑、封面、搜索与软删除均已验证 |
| Phase 5 日历 | 准备实现 |
| Phase 6 人物/地点 | 未开始 |
| Phase 7 回忆 | 未开始 |
| Phase 8 年度回忆 | 未开始 |
| Phase 9 最近删除 | 未开始 |
| Phase 10 备份恢复 | 未开始 |
| Phase 11 设置 | 未开始 |
| Phase 12 性能/异常 | 未开始 |
| Phase 13 最终 QA | 未开始 |
