package com.winterbot.modules

import com.winterbot.core.BaseModule
import com.winterbot.core.TemplateManager
import com.winterbot.GameAutomationService

/**
 * 邮件自动领取模块
 * 功能：
 * 1. 打开邮件界面
 * 2. 一键领取所有邮件奖励
 * 3. 返回主页
 *
 * 所需模板：
 * - mail_icon.png       邮件图标（主页入口）
 * - claim_all_mail.png  全部领取按钮
 * - mail_close.png      邮件关闭按钮
 * - confirm_btn.png     确认按钮
 */
class MailModule(service: GameAutomationService) : BaseModule(service) {

    override fun execute(): Boolean {
        log("开始领取邮件")

        // 步骤1: 先回主页
        goHome()
        sleep(500)

        // 步骤2: 找邮件图标并点击
        if (!clickTemplate("mail_icon", 0.78)) {
            log("未找到邮件图标，跳过")
            return false
        }
        sleep(2000)

        // 步骤3: 等待邮件界面加载，点"全部领取"
        var claimed = false
        for (attempt in 0 until 3) {
            if (clickTemplate("claim_all_mail", 0.75)) {
                claimed = true
                log("点击了全部领取")
                sleep(2000)
                break
            }
            sleep(1000)
        }

        // 步骤4: 处理领取后的确认弹窗
        if (claimed) {
            // 可能有"确认"或"好的"之类弹窗
            clickTemplate("confirm_btn", 0.75)
            sleep(1000)
            // 连续关闭弹窗
            for (i in 0 until 3) {
                if (clickTemplate("close_btn", 0.7)) {
                    sleep(600)
                } else {
                    break
                }
            }
        }

        // 步骤5: 关闭邮件界面
        if (!clickTemplate("mail_close", 0.75)) {
            back()
        }
        sleep(1000)
        goHome()

        log("邮件领取完成")
        return claimed
    }
}
