package com.winterbot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.core.content.ContextCompat

/**
 * 无尽冬日 · 游戏自动化服务
 * 基于 Android AccessibilityService 实现
 */
class GameAutomationService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var isBotRunning = false
    private var currentCycle = 0
    private var featureConfig = mutableMapOf<String, Boolean>()
    private var screenWidth = 1080
    private var screenHeight = 1920

    // 配置默认值
    private val defaultConfig = mapOf(
        "gather" to true,
        "upgrade" to true,
        "hunt" to true,
        "alliance" to true,
        "shield" to true,
        "tasks" to true,
        "mail" to true,
    )

    inner class LocalBinder {
        fun isRunning() = isBotRunning
        fun startBot(config: Map<String, Boolean>) = this@GameAutomationService.startBot(config)
        fun stopBot() = this@GameAutomationService.stopBot()
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?) = binder

    override fun onServiceConnected() {
        super.onServiceConnected()
        // 获取屏幕尺寸
        val display = android.hardware.display.DisplayManager::class.java
            .let { getSystemService(DISPLAY_SERVICE) as? android.hardware.display.DisplayManager }
            ?.getDisplay(0)
        val metrics = android.util.DisplayMetrics()
        display?.getRealMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels

        showToast("无障碍服务已连接")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 不需要处理事件，脚本自动执行
    }

    override fun onInterrupt() {
        stopBot()
    }

    // ═══════════════════════════════════════
    //  核心控制
    // ═══════════════════════════════════════

    private fun startBot(config: Map<String, Boolean>) {
        if (isBotRunning) return
        featureConfig = config.toMutableMap()
        // 填充默认值
        for ((k, v) in defaultConfig) {
            if (!featureConfig.containsKey(k)) featureConfig[k] = v
        }
        isBotRunning = true
        currentCycle = 0
        showToast("🤖 开始挂机")
        runCycle()
    }

    private fun stopBot() {
        isBotRunning = false
        handler.removeCallbacksAndMessages(null)
        showToast("⏹ 挂机已停止")
    }

    // ═══════════════════════════════════════
    //  主循环
    // ═══════════════════════════════════════

    private fun runCycle() {
        if (!isBotRunning) return
        currentCycle++
        log("═══ 第 $currentCycle 轮 ═══")

        val tasks = mutableListOf<() -> Unit>()

        if (featureConfig["mail"] == true) tasks.add { claimMail() }
        if (featureConfig["tasks"] == true) tasks.add { doTasks() }
        if (featureConfig["gather"] == true) tasks.add { collectResources() }
        if (featureConfig["upgrade"] == true) tasks.add { upgradeBuilding() }
        if (featureConfig["hunt"] == true) tasks.add { huntMonster() }
        if (featureConfig["alliance"] == true) tasks.add { allianceHelp() }
        if (featureConfig["shield"] == true) tasks.add { checkShield() }

        // 打乱顺序防检测
        tasks.shuffle()

        // 逐个执行
        executeTasksSequentially(tasks, 0)
    }

    private fun executeTasksSequentially(tasks: List<() -> Unit>, index: Int) {
        if (!isBotRunning || index >= tasks.size) {
            // 所有任务完成，等待下一轮
            if (isBotRunning) {
                handler.postDelayed({
                    runCycle()
                }, 20 * 60 * 1000L) // 20分钟
            }
            return
        }

        try {
            tasks[index]()
        } catch (e: Exception) {
            log("任务异常: ${e.message}")
        }

        // 随机延迟后执行下一个任务
        val delay = 2000 + (Math.random() * 3000).toInt()
        handler.postDelayed({
            executeTasksSequentially(tasks, index + 1)
        }, delay.toLong())
    }

    // ═══════════════════════════════════════
    //  操作函数
    // ═══════════════════════════════════════

    private fun click(x: Int, y: Int) {
        val jitter = (Math.random() * 10).toInt() - 5
        val path = Path().apply {
            moveTo(x + jitter.toFloat(), y + jitter.toFloat())
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gesture, null, null)
        sleep(500)
    }

    private fun sleep(ms: Int) {
        try { Thread.sleep(ms.toLong()) } catch (_: InterruptedException) {}
    }

    private fun log(msg: String) {
        android.util.Log.d("WinterBot", msg)
    }

    private fun showToast(msg: String) {
        handler.post { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
    }

    private fun vibrate() {
        try {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {}
    }

    private fun backPress() {
        performGlobalAction(GLOBAL_ACTION_BACK)
        sleep(500)
    }

    private fun goHome(times: Int = 4) {
        for (i in 0 until times) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            sleep(300)
        }
        sleep(1500)
    }

    private fun findText(text: String): AccessibilityNodeInfo? {
        return try {
            rootInActiveWindow?.findAccessibilityNodeInfosByText(text)
                ?.firstOrNull()
        } catch (_: Exception) { null }
    }

    private fun clickText(text: String): Boolean {
        val node = findText(text)
        if (node != null) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            click(rect.centerX(), rect.centerY())
            node.recycle()
            return true
        }
        return false
    }

    // ═══════════════════════════════════════
    //  功能实现
    // ═══════════════════════════════════════

    private fun collectResources() {
        log("💰 收取资源")
        // 右下角面板
        click(screenWidth - 80, screenHeight - 180)
        sleep(2000)
        clickText("全部收取")
        sleep(2000)
        backPress()
        sleep(500)
        goHome()
        log("✅ 资源已收取")
    }

    private fun upgradeBuilding() {
        log("🏗️ 升级建筑")
        click(screenWidth / 2, screenHeight / 2 - 50)
        sleep(2000)
        clickText("升级")
        sleep(2000)
        clickText("加速")
        sleep(1000)
        goHome()
        log("✅ 建筑升级")
    }

    private fun huntMonster() {
        log("🐾 扫野")
        clickText("地图")
        sleep(2000)
        click(screenWidth / 2, screenHeight / 3)
        sleep(1500)
        clickText("出征")
        sleep(2000)
        clickText("确定")
        sleep(1000)
        goHome()
        log("✅ 扫野完成")
    }

    private fun allianceHelp() {
        log("🤝 联盟")
        clickText("联盟")
        sleep(2000)
        for (i in 0 until 10) {
            if (!clickText("帮助")) break
            sleep(800)
        }
        clickText("捐献")
        sleep(1500)
        clickText("捐献")
        sleep(1000)
        goHome()
        log("✅ 联盟完成")
    }

    private fun checkShield() {
        log("🛡️ 护盾")
        clickText("背包")
        sleep(2000)
        clickText("护盾")
        sleep(1000)
        clickText("使用")
        sleep(1500)
        goHome()
        log("✅ 护盾检查完成")
    }

    private fun doTasks() {
        log("📋 任务")
        clickText("任务")
        sleep(2000)
        for (i in 0 until 10) {
            if (!clickText("领取")) break
            sleep(800)
        }
        goHome()
        log("✅ 任务完成")
    }

    private fun claimMail() {
        log("📧 邮件")
        clickText("邮件")
        sleep(2000)
        clickText("全部领取")
        sleep(1500)
        goHome()
        log("✅ 邮件已领")
    }
}
