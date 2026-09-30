# TomatoCleaner

三合一番茄系 App 净化 LSPosed 模块。

## 支持

| App | 包名 | 功能 |
|-----|------|------|
| 番茄畅听 | `com.xs.fm` | VIP 破解、去广告/弹窗/推广页 |
| 番茄免费小说 | `com.dragon.read` | 精简底部 tab、去悬浮球/金币挂件/推荐流 |
| 红果免费短剧 | `com.phoenix.read` | 精简底部 tab、去悬浮球/金币挂件 |

## 使用

1. 安装 APK
2. LSPosed 管理器启用本模块，勾选三个目标 App 作用域
3. 打开 TomatoCleaner 面板开关功能
4. 强制停止目标 App 后重新打开

## 技术

- 纯 Java，IXposedHookLoadPackage API 82
- VIP patch：反射 `AcctUserModel.isVip/freeAd`
- 广告拦截：Activity onResume 检测 + TextView.setText 过滤 + AdConfigManager 总闸
- UI 精简：在宿主方法层返回 null/false，让 View 根本不创建
