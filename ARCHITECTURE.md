# Mealory 架构

最后更新：2026-10-27

## 1. 总体结构

Mealory 是单 Activity、单 app module 的离线 Android 应用。

```text
Compose Screen
  → ViewModel (StateFlow / Coroutines)
    → Repository
      → Room / DataStore / App-private file storage
```

不引入 Hilt。依赖由 `MealoryApplication → AppContainer → Database / Repository / PhotoStorage / BackupManager` 显式构造。保持层次足够清晰，但不套用过度复杂的 Clean Architecture。

建议包结构：

```text
com.mealory.app
├── MainActivity.kt
├── MealoryApplication.kt
├── navigation/
├── ui/{onboarding,home,addmeal,mealdetail,calendar,memories,people,places,trash,settings,backup,components,theme}/
├── data/
│   ├── database/{entity,dao,MealoryDatabase.kt}
│   ├── repository/
│   └── preferences/
├── storage/{PhotoStorage.kt,PhotoProcessor.kt,ExifReader.kt}
├── backup/{BackupManager.kt,RestoreManager.kt,BackupModels.kt}
└── util/
```

## 2. 技术栈

- Kotlin、Jetpack Compose、Material 3
- Navigation Compose
- AndroidX ViewModel、StateFlow、Coroutines
- Room + Paging 3
- DataStore Preferences
- Coil
- WorkManager
- Activity Result API、Photo Picker、FileProvider、SAF
- JUnit、Room in-memory tests、Compose UI tests

基线：minSdk 26、compile/target SDK 37；AGP 9.4.1、Gradle 9.6.0。Manifest 不含 `INTERNET`。

## 3. Room 模型

### MealEntity

| 字段 | 类型 | 说明 |
|---|---|---|
| id | String UUID | 主键 |
| dateEpochDay | Long | 当地日期 |
| minuteOfDay | Int | 当地时间 0..1439 |
| mealType | String/enum converter | 餐次 |
| title | String? | 饭名 |
| note | String? | 备注 |
| placeId | String? | 可空外键，地点删除时 SET NULL |
| coverPhotoId | String? | 封面照片 |
| timeZoneId | String | 记录时当地时区 |
| createdAt | Long | 创建时间戳 |
| updatedAt | Long | 更新时间戳 |
| deletedAt | Long? | 软删除时间 |

索引：`dateEpochDay`、`deletedAt`、`placeId`，并按时间线查询需求评估组合索引 `(deletedAt, dateEpochDay, minuteOfDay)`。

### MealPhotoEntity

`id`、`mealId`、`relativePath`、`sortOrder`、`width`、`height`、`fileSize`、`mimeType`、`originalTakenAt`、`createdAt`。索引 `mealId`；Meal 永久删除时级联删除行，但真实文件由受控文件清理流程处理。`coverPhotoId` 的同餐照片约束由 Repository 在事务中校验，避免循环外键。

### PersonEntity / PlaceEntity

均保存 `id`、`name`、`createdAt`、`updatedAt`。名称在 Repository 归一化首尾与连续空白，并通过 `COLLATE NOCASE` 查询复用已有标签；不依赖通讯录、GPS 或地图服务。

### MealPersonCrossRef

复合主键 `(mealId, personId)`；分别索引 mealId 与 personId；任一端删除时只清关联。

## 4. 时间模型

UI 与查询以 `dateEpochDay + minuteOfDay + timeZoneId` 为真值。`createdAt/updatedAt/deletedAt` 使用 UTC epoch milliseconds 记录操作时间。这样用户跨时区后，历史 Meal 仍显示当时的当地日期和时间。

餐次默认由 minuteOfDay 推断，区间来自 DataStore；推断结果保存为明确 enum，后续区间变化不反向改写历史记录。

## 5. 查询与分页

- 正常时间线始终过滤 `deletedAt IS NULL`，按 `dateEpochDay DESC, minuteOfDay DESC, createdAt DESC`。
- 最近删除过滤 `deletedAt IS NOT NULL`。
- 首页由 Room PagingSource 驱动，日期头在 UI 层从分页项目派生。
- 搜索通过 Meal + Place + Person 关联和参数化 `LIKE`，对 `%`/`_` 做转义。
- Meal 详情使用按 ID 的 Room Flow；编辑、封面切换或软删除会使详情与首页 PagingSource 自动失效并刷新。
- 日历使用 `CalendarMealPreview` 时间范围投影，只读取 Meal ID、当地日期/时间、餐次、饭名与代表照片相对路径；月/年视图不会加载完整关系集合。
- 日期详情再通过按 `dateEpochDay` 的事务型 Flow 加载当天 `MealWithDetails`，并按当地时间升序显示。
- 人物/地点列表使用分组投影计算有效 Meal 数、首次和最近日期；软删除记录不参与统计。
- 人物/地点详情只查询对应的有效 Meal，沿用 Room 关系加载照片、地点和人物；记录页另有最近使用地点查询。
- “过去的今天”查询去年目标日期 ±3 天窗口，并按绝对日期差排序；UI 使用当前当地日期重新计算真实天数差。
- 随机回忆先计数 30 天以前的候选，再随机偏移读取一条；没有旧记录时才回退全部有效 Meal，避免全表随机排序。
- 历年同月使用 SQLite `strftime` 从 `dateEpochDay` 投影年份/月，仅读取 Meal ID、日期与代表照片路径后在 ViewModel 分组。
- 随机一顿优先 30 天以前；避免在超大表上长期使用昂贵的 `ORDER BY RANDOM()`，实现时采用计数 + 偏移或候选窗口。

## 6. 照片生命周期

```text
系统相机临时 URI / Photo Picker URI
  → 校验类型与数量
  → 读取 EXIF（时间、方向）
  → 解码时按目标尺寸采样
  → 旋转/翻转到正确方向
  → 标准或高质量编码到 staging 目录
  → fsync/校验
  → 数据库事务写 Meal + Photo 元数据
  → 原子移动到正式 app-private 路径
  → 清理临时文件
```

正式目录以相对路径存库，根目录由 PhotoStorage 统一解析。默认标准模式最长边约 2560 px、JPEG 约 90%；不得生成大量永久缩略图。Coil 负责内存/磁盘显示缓存。

当前实现将 staging 放在 `filesDir/pictures/.staging`，正式文件放在 `filesDir/pictures/meals/YYYY/MM/MEAL_UUID/PHOTO_UUID.jpg`。系统相机只获得 `cacheDir/camera` 的 FileProvider 临时 URI；保存、取消和页面销毁均清理临时文件。

软删除不移动或删图片；恢复只清 `deletedAt`。永久删除先确定精确文件清单，在数据库与文件系统间使用可恢复 staging/补偿策略，避免数据库已删但文件残留或反之。

## 7. DataStore

键：

- `onboardingCompleted: Boolean`
- `themeMode: SYSTEM | LIGHT | DARK`
- `photoQuality: STANDARD | HIGH`
- `lastBackupAt: Long?`
- `defaultMealTimeRanges: versioned JSON/proto-like value`

设置状态通过单一 PreferencesRepository 暴露 Flow；UI 不直接访问 DataStore。

## 8. 最近删除与后台清理

删除 Meal 只设置 `deletedAt`。永久删除前二次确认。WorkManager 使用唯一周期任务清理超过 30 天项目；Application 启动也触发一次幂等清理。清理任务必须可重试、可中断，并记录缺失文件但不因此破坏数据库一致性。

当前永久删除由 `DeletedMealManager` 协调：只接受 `deletedAt != null` 的 Meal，把存在的照片移动到 `pictures/.deleting/<mealId>`，再执行 Room 级联删除；数据库失败时恢复原路径，成功后清除隔离文件和空 Meal 目录。`DeletedMealsCleanupWorker` 的启动任务和 24 小时周期任务复用同一入口，任一条失败会请求重试。

## 9. 备份格式

备份不是数据库文件快照，而是版本化开放格式：

```text
manifest.json
data/meals.json
data/photos.json
data/people.json
data/places.json
data/meal_people.json
data/settings.json
photos/<UUID>.jpg
```

manifest 保存格式版本、App 版本、UTC 创建时间、各实体计数、照片 SHA-256。JSON 使用稳定字段名、UTF-8 和明确的可空语义。

备份时先在 app cache/staging 生成并验证 ZIP，再通过 SAF 写用户选择的 Document URI。取消或失败时删除 staging，不更新 `lastBackupAt`。

## 10. 原子恢复

V1 仅完整替换：

1. SAF 读取 ZIP 到受限 staging。
2. 防 Zip Slip、文件数/大小/压缩比攻击。
3. 验证 manifest、formatVersion、JSON schema、UUID/关系、计数、照片类型和 SHA-256。
4. 在 staging 建新 Room 数据库与新照片树，并运行完整一致性检查。
5. 用户确认后关闭当前数据库。
6. 将当前数据改名为 rollback 区，新数据原子改名为正式区。
7. 重新打开并验证；成功才删除 rollback，失败立即回滚。

恢复全过程不得直接覆盖正在使用的数据。

## 11. 隐私与安全

- `android:allowBackup="false"`；云备份与设备迁移规则排除全部域。
- 不声明网络、定位、通讯录、全相册读取权限。
- FileProvider 只暴露临时相机目录，使用最小 URI 权限并及时撤销。
- 所有外部文件/URI 都视为不可信输入，限制尺寸、MIME、解码资源与 ZIP 路径。
- 日志不输出用户饭名、备注、人物、地点或文件路径等敏感内容。

## 12. 测试策略

- 纯单元：餐次推断、时间/日期、回忆候选、统计、备份模型验证。
- Room JVM/instrumented：CRUD、关系、索引查询、软删除/恢复、分页、搜索、日历范围/日期查询、大数据。
- 文件测试：EXIF、方向、标准/高质量、1/9 张、缺失/损坏文件。
- 备份恢复：往返等价、损坏 JSON/ZIP、哈希错误、关系错误、中断与回滚。
- Compose UI：Onboarding、四 Tab、空首页、记录入口、首页到详情、编辑保存和删除确认。
- 设备：模拟器做基础回归；真机验证系统相机、Photo Picker、SAF 与完整恢复。

## 13. 年度回忆统计

年度页使用 `[year-01-01, nextYear-01-01)` 的半开 epoch-day 范围加载目标年有效 Meal，并按日期、分钟和创建时间稳定升序排列。记录日数在目标年内去重日期；首末餐直接来自稳定排序结果。

全局里程碑不加载全年之前的关系数据，而是先查询年初以前的有效 Meal 数，再加目标年内的稳定序号。人物和地点从目标年关系聚合；只有最高频次至少为 2 才生成“最常”结论，并列项全部保留。四季拼图只选择真实封面或首张照片，每季最多四张。

