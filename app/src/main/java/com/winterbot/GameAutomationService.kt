package com.winterbot

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.winterbot.core.ImageMatcher
import com.winterbot.core.ScreenCapturer
import com.winterbot.core.TemplateManager
import com.winterbot.modules.GatherModule
import com.winterbot.modules.MailModule
import com.winterbot.modules.ResourceModule
import com.winterbot.modules.TaskModule
import com.winterbot.ui.FloatWindowManager

/**
 * 无尽冬日 · 游戏自动化服务
 * 基于 AccessibilityService + MediaProjection + OpenCV 图像识别
 *
 * 架构：
 * - 核心层：截图、图像匹配、模板管理
 * - 模块层：各功能独立模块继承 BaseModule
 * - 调度层：主循环按优先级轮询执行各模块
 */
class GameAutomationService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var isBotRunning = false
    private var currentCycle = 0
    private var featureConfig = mutableMapOf<String, Boolean>()

    var screenWidth = 1080
    var screenHeight = 1920
    var screenCapturer: ScreenCapturer? = null

    // 功能模块
    private lateinit var resourceModule: ResourceModule
    private lateinit var mailModule: MailModule
    private lateinit var taskModule: TaskModule
    private lateinit var gatherModule: GatherModule

    // 悬浮窗
    private var floatWindow: FloatWindowManager? = null

    // 默认配置
    private val defaultConfig = mapOf(
        "gather" to true,
        "mail" to true,
        "tasks" to true,
        "resource" to true,
        "upgrade" to false,
        "hunt" to false,
        "alliance" to false,
        "shield" to false,
    )

    inner class LocalBinder {
        fun isRunning() = isBotRunning
        fun startBot(config: Map<String, Boolean>) = this@GameAutomationService.startBot(config)
        fun stopBot() = this@GameAutomationService.stopBot()
        fun hasScreenCapture() = screenCapturer != null
        fun setScreenCapturer(capturer: ScreenCapturer) {
            this@GameAutomationService.screenCapturer = capturer
        }
        fun updateScreenSize(w: Int, h: Int) {
            screenWidth = w
            screenHeight = h
        }
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?) = binder

    override fun onServiceConnected() {
        super.onServiceConnected()

        // 获取屏幕尺寸
        val display = getSystemService(DISPLAY_SERVICE) as? android.hardware.display.DisplayManager
        display?.getDisplay(0)?.let {
            val metrics = android.util.DisplayMetrics()
            it.getRealMetrics(metrics)
            screenWidth = metrics.widthPixels
            screenHeight = metrics.heightPixels
        }

        // 初始化 OpenCV
        ImageMatcher.init()

        // 加载模板
        TemplateManager.loadAll(this)

        // 初始化模块
        resourceModule = ResourceModule(this)
        mailModule = MailModule(this)
        taskModule = TaskModule(this)
        gatherModule = GatherModule(this)

        // 初始化悬浮窗
        floatWindow = FloatWindowManager(this)

        Log.d("WinterBot", "服务已连接，屏幕: ${screenWidth}x${screenHeight}")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 事件监听（可选，用于弹窗检测等）
    }

    override fun onInterrupt() {
        stopBot()
    }

    // ═══════════════════════════════════════
    //  核心控制
    // ═══════════════════════════════════════

    private fun startBot(config: Map<String, Boolean>) {
        if (isBotRunning) return

        if (screenCapturer == null) {
            Log.e("WinterBot", "未初始化截屏服务，无法启动")
            return
        }

        featureConfig = config.toMutableMap()
        for ((k, v) in defaultConfig) {
            if (!featureConfig.containsKey(k)) featureConfig[k] = v
        }

        isBotRunning = true
        currentCycle = 0

        floatWindow?.show()
        floatWindow?.updateStatus("🤖 挂机中")

        Log.d("WinterBot", "开始挂机")
        runCycle()
    }

    private fun stopBot() {
        isBotRunning = false
        handler.removeCallbacksAndMessages(null)
        floatWindow?.updateStatus("⏸ 已停止")
        Log.d("WinterBot", "挂机已停止")
    }

    // ═══════════════════════════════════════
    //  主循环调度
    // ═══════════════════════════════════════

    private fun runCycle() {
        if (!isBotRunning) return
        currentCycle++
        updateStatus("═══ 第 $currentCycle 轮 ═══")
        Log.d("WinterBot", "=== 第 $currentCycle 轮开始 ===")

        // 按优先级构建任务列表
        val tasks = mutableListOf<Pair<String, () -> Boolean>>()

        // 高频任务优先
        if (featureConfig["mail"] == true) tasks.add("邮件" to { mailModule.execute() })
        if (featureConfig["tasks"] == true) tasks.add("任务" to { taskModule.execute() })
        if (featureConfig["resource"] == true) tasks.add("资源" to { resourceModule.execute() })
        if (featureConfig["gather"] == true) tasks.add("采集" to { gatherModule.execute() })

        // 打乱顺序防检测
        tasks.shuffle()

        executeTasksSequentially(tasks, 0)
    }

    private fun executeTasksSequentially(
        tasks: List<Pair<String, () -> Boolean>>,
        index: Int
    ) {
        if (!isBotRunning) return
        if (index >= tasks.size) {
            // 一轮结束，等待下一轮
            val nextDelay = 15 * 60 * 1000 + (Math.random() * 5 * 60 * 1000).toLong()
            updateStatus("💤 本轮完成，${nextDelay / 60000} 分钟后开始下一轮")
            handler.postDelayed({ runCycle() }, nextDelay)
            return
        }

        val (name, task) = tasks[index]
        updateStatus("🔄 执行: $name")

        try {
            val result = task()
            Log.d("WinterBot", "$name 执行结果: $result")
        } catch (e: Exception) {
            Log.e("WinterBot", "$name 执行异常: ${e.message}", e)
            updateStatus("⚠️ $name 异常: ${e.message}")
        }

        // 任务间随机延迟
        val delay = 3000 + (Math.random() * 5000).toLong()
        handler.postDelayed({
            executeTasksSequentially(tasks, index + 1)
        }, delay)
    }

    // ═══════════════════════════════════════
    //  状态更新
    // ═══════════════════════════════════════

    fun updateStatus(text: String) {
        floatWindow?.updateStatus(text)
        Log.d("WinterBot", "状态: $text")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBot()
        floatWindow?.hide()
        screenCapturer?.release()
        TemplateManager.release()
    }

    companion object {
        var instance: GameAutomationService? = null
            private set
    }

    init {
        instance = this
    }
}
