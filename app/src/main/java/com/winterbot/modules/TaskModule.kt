package com.winterbot.modules

import com.winterbot.core.BaseModule
import com.winterbot.core.TemplateManager
import com.winterbot.GameAutomationService

/**
 * 每日任务自动领取模块
 * 功能：
 * 1. 打开任务面板
 * 2. 一键领取所有已完成任务奖励
 * 3. 领取活跃度宝箱
 * 4. 返回主页
 *
 * 所需模板：
 * - task_icon.png       任务图标（主页入口）
 * - task_claim.png      任务领取按钮
 * - chest_claim.png     宝箱领取按钮
 * - task_close.png      任务面板关闭按钮
 * - tab_daily.png       每日任务标签页
 * - tab_active.png      活跃度标签页
 */
class TaskModule(service: GameAutomationService) : BaseModule(service) {

    override fun execute(): Boolean {
        log("开始领取任务奖励")

        // 步骤1: 回主页
        goHome()
        sleep(500)

        // 步骤2: 打开任务面板
        if (!clickTemplate("task_icon", 0.78)) {
            log("未找到任务图标，跳过")
            return false
        }
        sleep(2000)

        var claimed = 0

        // 步骤3: 切到每日任务标签
        if (TemplateManager.has("tab_daily")) {
            clickTemplate("tab_daily", 0.75)
            sleep(1000)
        }

        // 步骤4: 循环点"领取"按钮（可能有多个）
        for (round in 0 until 5) {
            val screen = screenshot()
            val template = TemplateManager.get("task_claim")
            if (screen == null || template == null) break

            val rects = com.winterbot.core.ImageMatcher.findAllTemplates(
                screen, template, 0.78
            )
            screen.recycle()

            if (rects.isEmpty()) break

            // 逐个点击领取
            for (rect in rects) {
                clickRect(rect)
                sleep(800)
                // 关闭领取成功弹窗
                clickTemplate("confirm_btn", 0.7)
                sleep(500)
                claimed++
            }
            log("第 $round 轮领取了 ${rects.size} 个任务")
        }

        // 步骤5: 切到活跃度标签，领宝箱
        if (TemplateManager.has("tab_active")) {
            clickTemplate("tab_active", 0.75)
            sleep(1000)

            for (round in 0 until 3) {
                if (!clickTemplate("chest_claim", 0.75)) break
                sleep(1200)
                clickTemplate("confirm_btn", 0.7)
                sleep(600)
                claimed++
            }
        }

        // 步骤6: 关闭任务面板
        if (!clickTemplate("task_close", 0.75)) {
            back()
        }
        sleep(800)
        goHome()

        log("任务领取完成，共领取 $claimed 项")
        return claimed > 0
    }
}
