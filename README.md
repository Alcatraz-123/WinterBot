# ❄ 无尽冬日助手 — WinterBot

基于 Android 无障碍服务 + OpenCV 图像识别的游戏自动化辅助工具。

## ✨ 功能特性

### 已实现模块
| 模块 | 状态 | 说明 |
|------|------|------|
| 💰 城内资源收取 | ✅ 完成 | 自动收取所有资源建筑产出、离线收益、全部收取 |
| 📧 邮件自动领取 | ✅ 完成 | 一键领取所有邮件附件奖励 |
| 📋 每日任务领取 | ✅ 完成 | 任务奖励、活跃度宝箱一键清空 |
| ⛏️ 野外资源采集 | ✅ 完成 | 自动搜索资源点、派部队采集、按优先级搜索 |
| 🏗️ 建筑升级 | 🔲 待开发 | 自动升级建筑 |
| 🐾 自动扫野 | 🔲 待开发 | 自动打野外怪物 |
| 🤝 联盟互助 | 🔲 待开发 | 联盟帮助、捐献、商店兑换 |
| 🛡️ 自动护盾 | 🔲 待开发 | 护盾检测与续期 |

### 核心能力
- **图像识别**：OpenCV 模板匹配，不受游戏 UI 层级限制
- **防检测**：随机点击偏移、随机操作间隔、任务顺序打乱、随机延迟
- **悬浮窗**：实时显示挂机状态，可拖动靠边吸附
- **模块化架构**：每个功能独立模块，方便扩展和维护
- **容错机制**：找不到元素自动跳过，不崩溃

## 📱 使用方法

### 1. 授予三项权限
打开 APP 后依次点击授权：
1. **无障碍服务** → 找到「无尽冬日助手」→ 开启
2. **悬浮窗权限** → 允许显示悬浮窗
3. **屏幕截图权限** → 允许屏幕录制（仅用于本地图像识别）

### 2. 放入模板图片
将游戏界面截图裁剪后的模板图片放入手机：
`/Android/data/com.winterbot/files/templates/`

> 模板图片清单见 `app/src/main/assets/templates/README.md`
> 没有模板的功能会自动跳过，不会崩溃

### 3. 选择功能 → 开始挂机
勾选需要的功能，点击「开始挂机」。
悬浮球会显示当前执行状态。

## 🏗️ 技术架构

```
GameAutomationService (无障碍服务，主调度)
├── core/
│   ├── ImageMatcher.kt      # OpenCV 模板匹配
│   ├── ScreenCapturer.kt    # MediaProjection 截屏
│   ├── TemplateManager.kt   # 模板图管理
│   └── BaseModule.kt        # 功能模块基类
├── modules/
│   ├── ResourceModule.kt    # 城内资源收取
│   ├── MailModule.kt        # 邮件领取
│   ├── TaskModule.kt        # 任务领取
│   └── GatherModule.kt      # 野外采集
└── ui/
    └── FloatWindowManager.kt # 悬浮窗
```

### 扩展新功能
继承 `BaseModule`，实现 `execute()` 方法即可：

```kotlin
class MyModule(service: GameAutomationService) : BaseModule(service) {
    override fun execute(): Boolean {
        // 使用 clickTemplate / findTemplate / waitForTemplate 等
        clickTemplate("some_button")
        return true
    }
}
```

然后在 `GameAutomationService.kt` 中注册到主循环。

## 🔧 编译方式

### Android Studio
1. 安装 Android Studio
2. 打开本项目文件夹
3. Build → Build APK(s)

### GitHub Actions
推送到 GitHub 后自动编译（需配置）。

## ⚠️ 免责声明

- 本工具仅用于学习 Android 自动化、图像识别技术
- 使用脚本外挂违反游戏用户协议，可能导致账号封禁
- 请合理使用，风险自负
- 建议仅用收取资源等低风险功能，避免打架、掠夺等高风险操作
