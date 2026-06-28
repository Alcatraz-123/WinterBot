package com.winterbot.modules

import android.graphics.Rect
import com.winterbot.core.BaseModule
import com.winterbot.core.ImageMatcher
import com.winterbot.core.TemplateManager
import com.winterbot.GameAutomationService

/**
 * 野外资源采集模块
 * 功能：
 * 1. 打开世界地图
 * 2. 搜索资源点（木材/铁矿/原油/粮食）
 * 3. 派部队采集
 * 4. 部队满载自动召回
 * 5. 被攻击自动召回
 *
 * 所需模板：
 * - map_icon.png        世界地图按钮
 * - wood_icon.png       木材资源点
 * - iron_icon.png       铁矿资源点
 * - oil_icon.png        原油资源点
 * - food_icon.png       粮食资源点
 * - gather_btn.png      采集按钮
 * - march_btn.png       出征/行军按钮
 * - recall_btn.png      召回按钮
 * - confirm_march.png      确认出征
 * - back_city.png        返回基地
 * - troop_full.png       部队满载/满载返回
 */
class GatherModule(service: GameAutomationService) : BaseModule(service) {

    // 采集资源优先级（按顺序搜索）
    val resourcePriority = listOf("oil", "iron", "wood", "food")

    override fun execute(): Boolean {
        log("开始野外采集巡检")

        // 步骤1: 回主城
        goHome()
        sleep(500)

        // 步骤2: 检查是否有部队已经在采集（部队回来的提示）
        checkReturningTroops()

        // 步骤3: 打开世界地图
        if (!openWorldMap()) {
            log("无法打开世界地图")
            return false
        }

        // 步骤4: 按优先级找空闲的采集位
        var gathered = 0
        for (resourceType in resourcePriority) {
            if (gathered >= 3) break // 最多派3队（可配置）

            val iconName = "${resourceType}_icon"
            if (!TemplateManager.has(iconName)) continue

            // 扫图找资源点
            val resourcePoint = scanForResource(iconName)
            if (resourcePoint != null) {
                log("找到 ${resourceName(resourceType)} 资源点")
                if (sendGatherTroop(resourcePoint)) {
                    gathered++
                    sleep(2000)
                }
            }

            // 滑动地图换个位置继续找
            swipeMap()
            sleep(1000)
        }

        // 步骤5: 回主城
        backToCity()

        log("采集巡检完成，派出 $gathered 队")
        return gathered > 0
    }

    /**
     * 打开世界地图
     */
    private fun openWorldMap(): Boolean {
        if (clickTemplate("map_icon", 0.78)) {
            sleep(2500)
            return true
        }
        return false
    }

    /**
     * 扫描寻找资源点
     * 在当前可见地图范围内查找指定类型的资源点
     */
    private fun scanForResource(templateName: String): Rect? {
        val template = TemplateManager.get(templateName) ?: return null
        // 在地图中央区域搜索（排除UI边栏）
        val mapRegion = Rect(
            100, 300,
            screenWidth - 100, screenHeight - 400
        )

        for (attempt in 0 until 3) {
            val screen = screenshot() ?: continue
            val rect = ImageMatcher.findTemplateInRegion(screen, template, mapRegion, 0.72)
            screen.recycle()
            if (rect != null) return rect

            // 没找到就滑动一下地图
            swipeMap()
            sleep(1200)
        }
        return null
    }

    /**
     * 派出采集部队
     */
    private fun sendGatherTroop(resourcePoint: Rect): Boolean {
        // 点击资源点
        clickRect(resourcePoint)
        sleep(1500)

        // 点击采集按钮
        if (!clickTemplate("gather_btn", 0.75)) {
            log("没有采集按钮，可能是玩家基地或已被占领")
            back()
            sleep(500)
            return false
        }
        sleep(1200)

        // 确认出征
        if (clickTemplate("confirm_march", 0.75)) {
            sleep(1000)
            log("部队已出征采集")
            return true
        }

        // 有些游戏可能直接点"出征"
        if (clickTemplate("march_btn", 0.75)) {
            sleep(1000)
            return true
        }

        back()
        sleep(500)
        return false
    }

    /**
     * 检查是否有部队正在返回（满载/被打回来的，需要重新派出去）
     */
    private fun checkReturningTroops() {
        // 检测部队满载返回的图标，有就等下
        if (hasTemplate("troop_full", 0.75)) {
            log("检测到部队正在返回，等待中...")
            sleep(5000)
        }
    }

    /**
     * 滑动地图（随机方向）
     */
    private fun swipeMap() {
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2
        val direction = (Math.random() * 4).toInt()
        val distance = 300 + (Math.random() * 200).toInt()

        when (direction) {
            0 -> swipe(centerX, centerY, centerX, centerY - distance) // 向上滑（地图下移）
            1 -> swipe(centerX, centerY, centerX, centerY + distance) // 向下
            2 -> swipe(centerX, centerY, centerX - distance, centerY) // 向左
            3 -> swipe(centerX, centerY, centerX + distance, centerY) // 向右
        }
    }

    /**
     * 返回主城
     */
    private fun backToCity() {
        if (clickTemplate("back_city", 0.75)) {
            sleep(1500)
        } else {
            back(2)
            sleep(1000)
        }
    }

    private fun resourceName(type: String) = when (type) {
        "oil" -> "原油"
        "iron" -> "铁矿"
        "wood" -> "木材"
        "food" -> "粮食"
        else -> type
    }
}
