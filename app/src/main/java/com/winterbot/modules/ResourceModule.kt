package com.winterbot.modules

import com.winterbot.core.BaseModule
import com.winterbot.core.ImageMatcher.findTemplate
import com.winterbot.core.ImageMatcher.hasTemplate
import com.winterbot.core.TemplateManager
import com.winterbot.GameAutomationService

/**
 * 城内资源收取模块
 * 功能：
 * 1. 收取所有资源建筑产出（农田/伐木场/铁矿/油井等）
 * 2. 收取右下角资源面板的"全部收取"
 * 3. 领取离线收益
 *
 * 所需模板（assets/templates/）：
 * - collect_btn.png     收取按钮（资源建筑上方的收集图标）
 * - claim_all.png       全部收取按钮
 * - offline_reward.png  离线收益弹窗
 * - claim_btn.png       领取按钮
 * - close_btn.png       关闭按钮(X)
 */
class ResourceModule(service: GameAutomationService) : BaseModule(service) {

    override fun execute(): Boolean {
        log("开始收取城内资源")

        var collected = 0

        // 步骤1: 先领离线收益（如果弹窗存在）
        if (hasTemplate("offline_reward", 0.75)) {
            clickTemplate("claim_btn", 0.8)
            sleep(1500)
            // 关闭可能的二级弹窗
            clickTemplate("close_btn", 0.75)
            sleep(800)
            log("领取了离线收益")
            collected++
        }

        // 步骤2: 滑动屏幕找资源建筑的收取按钮
        // 游戏城内视角，从上到下滑动扫描
        val swipeRegions = listOf(
            // 上半屏
            Pair(screenHeight / 3, screenHeight / 2),
            // 下半屏
            Pair(screenHeight / 2, screenHeight / 3),
        )

        for ((startY, endY) in swipeRegions) {
            // 在当前屏幕找收取按钮
            val found = findAllCollectButtons()
            for (btn in found) {
                clickRect(btn)
                sleep(600)
                collected++
            }

            if (collected > 0) {
                // 点击空白处关闭弹出的面板
                click(screenWidth / 2, screenHeight / 2)
                sleep(500)
            }

            // 滑动到下一个视角
            swipe(
                screenWidth / 2, startY,
                screenWidth / 2, endY,
                500
            )
            sleep(800)
        }

        // 步骤3: 尝试点右下角"全部收取"（资源面板）
        // 右下角区域
        val rightBottomRegion = android.graphics.Rect(
            screenWidth - 250, screenHeight - 350,
            screenWidth - 50, screenHeight - 100
        )
        if (TemplateManager.has("claim_all")) {
            val screen = screenshot()
            if (screen != null) {
                val template = TemplateManager.get("claim_all")
                val rect = template?.let {
                    com.winterbot.core.ImageMatcher.findTemplateInRegion(
                        screen, it, rightBottomRegion, 0.75
                    )
                }
                screen.recycle()
                if (rect != null) {
                    clickRect(rect)
                    sleep(1500)
                    log("点击了全部收取")
                    collected++
                    // 关闭资源面板
                    back()
                }
            }
        }

        log("城内资源收取完成，共收取 $collected 处")
        return collected > 0
    }

    /**
     * 找到屏幕上所有的收取按钮
     */
    private fun findAllCollectButtons(): List<android.graphics.Rect> {
        val template = TemplateManager.get("collect_btn") ?: return emptyList()
        val screen = screenshot() ?: return emptyList()
        val results = com.winterbot.core.ImageMatcher.findAllTemplates(
            screen, template, 0.78
        )
        screen.recycle()
        return results
    }
}
