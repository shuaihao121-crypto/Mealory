# Mealory 1.0.0 正式包记录

- 生成时间：2026-10-31 15:00（Asia/Shanghai）
- 包名：`com.mealory.app`
- 版本：`1.0.0`（versionCode 1）
- 文件：`release-output/apk/Mealory-1.0.0-release.apk`
- 文件大小：15,532,035 bytes
- APK SHA-256：`E79A8AEE5ED755010662C376707AB8426EE02BB2EA6090C8EC6566102313CCD0`
- 签名证书 SHA-256：`571572A751313DA464141ABD6E845FA277552D984C118371B23699B8528E4D8D`
- 签名方案：APK Signature Scheme v2
- 隐私政策：`https://mealory-privacy-baizihao.daringguppy2051.chatgpt.site`

## 已验证

- `testDebugUnitTest` 通过
- `lintDebug` 与 Release vital lint 通过
- `assembleRelease` 通过
- `apksigner verify --verbose --print-certs` 通过
- 正式包在 Android API 37 模拟器安装成功
- 首次隐私页面显示运营者“白子豪”、同意与拒绝入口
- 首次引导完成后四个主导航入口正常显示
- 正式版冷启动成功，版本信息为 `1.0.0` / `1`

正式密钥、密码和 `keystore.properties` 不进入 Git。发布前必须将密钥与密码至少备份到两个安全位置。
