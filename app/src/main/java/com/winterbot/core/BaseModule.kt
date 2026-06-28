package com.winterbot.core

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import com.winterbot.GameAutomationService

/**
 * 游戏自动化操作基类
 * 提供截图、点击、滑动、等待等基础操作
 * 所有功能模块继承此类
 */
abstract class BaseModule(protected val service: GameAutomationService) {

    protected val screenCapturer: ScreenCapturer?
        get() = service.screenCapturer

    protected val screenWidth: Int
        get() = service.screenWidth

    protected val screenHeight: Int
        get() = service.screenHeight

    protected val name: String = this.javaClass.simpleName

    // ===== 基础操作 =====

    /**
     * 截取当前屏幕
     */
    protected fun screenshot(): android.graphics.Bitmap? {
        return screenCapturer?.capture()
    }

    /**
     * 点击坐标（带随机偏移防检测）
     */
    protected fun click(x: Int, y: Int, jitter: Int = 8) {
        val jx = x + (Math.random() * jitter * 2 - jitter).toInt()
        val jy = y + (Math.random() * jitter * 2 - jitter).toInt()

        // 随机滑动路径模拟真人
        val path = Path().apply {
            moveTo(jx.toFloat(), jy.toFloat())
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(
                path, 0, 80 + (Math.random() * 60).toLong()
            ))
            .build()
        service.dispatchGesture(gesture, null, null)
        sleep(300 + (Math.random() * 200).toInt())
    }

    /**
     * 点击矩形中心
     */
    protected fun clickRect(rect: Rect) {
        click(rect.centerX(), rect.centerY())
    }

    /**
     * 查找模板并点击
     * @return 是否点击成功
     */
    protected fun clickTemplate(
        templateName: String,
        threshold: Double = 0.8,
        region: Rect? = null
    ): Boolean {
        val screen = screenshot() ?: return false
        val template = TemplateManager.get(templateName) ?: return false

        val rect = if (region != null) {
            ImageMatcher.findTemplateInRegion(screen, template, region, threshold)
        } else {
            ImageMatcher.findTemplate(screen, template, threshold)
        }

        if (rect != null) {
            clickRect(rect)
            screen.recycle()
            return true
        }
        screen.recycle()
        return false
    }

    /**
     * 等待模板出现
     * @param timeout 超时时间（毫秒）
     * @return 出现了返回true，超时返回false
     */
    protected fun waitForTemplate(
        templateName: String,
        timeout: Int = 5000,
        threshold: Double = 0.8,
        region: Rect? = null
    ): Boolean {
        val template = TemplateManager.get(templateName) ?: return false
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeout) {
            val screen = screenshot()
            if (screen != null) {
                val rect = if (region != null) {
                    ImageMatcher.findTemplateInRegion(screen, template, region, threshold)
                } else {
                    ImageMatcher.findTemplate(screen, template, threshold)
                }
                screen.recycle()
                if (rect != null) return true
            }
            sleep(500)
        }
        return false
    }

    /**
     * 等待模板消失
     */
    protected fun waitForTemplateGone(
        templateName: String,
        timeout: Int = 5000,
        threshold: Double = 0.8
    ): Boolean {
        val template = TemplateManager.get(templateName) ?: return true
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeout) {
            val screen = screenshot()
            if (screen != null) {
                val rect = ImageMatcher.findTemplate(screen, template, threshold)
                screen.recycle()
                if (rect == null) return true
            }
            sleep(500)
        }
        return false
    }

    /**
     * 检查当前页面是否有某模板
     */
    protected fun hasTemplate(templateName: String, threshold: Double = 0.8): Boolean {
        val screen = screenshot() ?: return false
        val template = TemplateManager.get(templateName) ?: return false
        val result = ImageMatcher.hasTemplate(screen, template, threshold)
        screen.recycle()
        return result
    }

    /**
     * 查找模板位置
     */
    protected fun findTemplate(
        templateName: String,
        threshold: Double = 0.8
    ): Rect? {
        val screen = screenshot() ?: return null
        val template = TemplateManager.get(templateName) ?: return null
        val result = ImageMatcher.findTemplate(screen, template, threshold)
        screen.recycle()
        return result
    }

    /**
     * 滑动操作
     */
    protected fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, duration: Int = 400) {
        val path = Path().apply {
            moveTo(x1.toFloat(), y1.toFloat())
            lineTo(x2.toFloat(), y2.toFloat())
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration.toLong()))
            .build()
        service.dispatchGesture(gesture, null, null)
        sleep(duration + 200)
    }

    /**
     * 返回键
     */
    protected fun back(times: Int = 1) {
        repeat(times) {
            service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            sleep(500)
        }
    }

    /**
     * 回到游戏主页（连续返回）
     */
    protected fun goHome() {
        repeat(5) {
            service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            sleep(400)
        }
        sleep(1000)
    }

    protected fun sleep(ms: Int) {
        try { Thread.sleep(ms.toLong()) } catch (_: InterruptedException) {}
    }

    protected fun log(msg: String) {
        Log.d("WinterBot", "[$name] $msg")
        service.updateStatus("[$name] $msg")
    }

    /**
     * 模块执行入口
     * @return 是否执行成功
     */
    abstract fun execute(): Boolean
}
