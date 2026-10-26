# Mealory

Mealory 是一款只属于用户自己的 Android 饮食回忆日记。

> Meal + Memory — 把吃过的每一顿饭，留给未来的自己。
>
> **MEALS TODAY, MEMORIES TOMORROW**

## 产品原则

- 私人、温暖、安静、长期、真实、克制。
- 照片 > 日期 > 回忆 > 数据；不做热量、营养、打卡或社交。
- V1 无账号、无服务器、无广告、无订阅、无 AI、无网络权限。
- 照片和数据库只保存在 App 私有目录；系统云备份明确关闭。
- 用户可导出开放、可解压、长期可理解的 ZIP 完整备份。

## 当前技术基线

- Kotlin + Jetpack Compose + Material 3
- Single Activity + Navigation Compose
- minSdk 26 / compileSdk 37 / targetSdk 37
- Android Gradle Plugin 9.4.1 / Gradle 9.6.0
- 单 `app` module

后续阶段按 [ARCHITECTURE.md](ARCHITECTURE.md) 引入 ViewModel、StateFlow、Coroutines、Room、DataStore、Coil、Paging 3、WorkManager、Photo Picker、FileProvider 与 Storage Access Framework。

## 本地构建

项目使用 Android Studio 自带 JDK，不要求系统另装 Java 或 Gradle。

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat connectedDebugAndroidTest
```

Debug APK 生成在 `app/build/outputs/apk/debug/app-debug.apk`。

## 文档

- [PRD.md](PRD.md)：冻结的 V1 产品范围与验收要求。
- [ARCHITECTURE.md](ARCHITECTURE.md)：数据、照片、备份与模块设计。
- [DECISIONS.md](DECISIONS.md)：重要技术决策及理由。
- [PROJECT_STATUS.md](PROJECT_STATUS.md)：当前进度、已知问题和下一步。

## 隐私提醒

卸载 Mealory 或清除应用数据会删除本地记录。正式数据功能完成后，用户应在卸载或清除数据前从“数据与备份”导出完整备份。

