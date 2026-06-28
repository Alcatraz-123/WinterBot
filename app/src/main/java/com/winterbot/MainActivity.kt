package com.winterbot

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textview.MaterialTextView
import com.winterbot.core.ImageMatcher
import com.winterbot.core.ScreenCapturer
import com.winterbot.core.TemplateManager

/**
 * 主界面
 * 流程：
 * 1. 开启无障碍服务
 * 2. 授予悬浮窗权限
 * 3. 授予截屏权限（MediaProjection）
 * 4. 选择功能，启动挂机
 */
class MainActivity : AppCompatActivity() {

    private var serviceBinder: GameAutomationService.LocalBinder? = null
    private var serviceConnected = false
    private var screenCapturer: ScreenCapturer? = null

    private lateinit var startBtn: MaterialButton
    private lateinit var statusText: MaterialTextView
    private lateinit var featureSwitches: MutableMap<String, SwitchMaterial>

    private val REQUEST_MEDIA_PROJECTION = 1001
    private val REQUEST_OVERLAY = 1002

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            serviceBinder = service as GameAutomationService.LocalBinder
            serviceConnected = true
            updateUIState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBinder = null
            serviceConnected = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化 OpenCV
        ImageMatcher.init()
        // 加载模板
        TemplateManager.loadAll(this)

        setupUI()
        bindService()
        checkPermissions()
    }

    private fun setupUI() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
            setBackgroundColor(0xFFF5F5F5.toInt())
            isScrollContainer = true
        }

        val scrollView = ScrollView(this).apply {
            addView(root)
        }

        // 标题
        root.addView(TextView(this).apply {
            text = "❄ 无尽冬日助手"
            textSize = 28f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(0xFF1A237E.toInt())
            setPadding(0, 0, 0, 8)
        })

        root.addView(TextView(this).apply {
            text = "基于图像识别的智能挂机辅助"
            textSize = 14f
            setTextColor(0xFF757575.toInt())
            setPadding(0, 0, 0, 24)
        })

        // 状态指示
        statusText = MaterialTextView(this).apply {
            text = "● 服务未启动"
            textSize = 16f
            setTextColor(0xFF9E9E9E.toInt())
            setPadding(0, 0, 0, 16)
        }
        root.addView(statusText)

        // ===== 权限引导卡片 =====
        addPermissionCard(root, "🔓 无障碍服务", "用于执行手势操作", false) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        addPermissionCard(root, "📱 悬浮窗权限", "显示挂机状态悬浮球", false) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:$packageName")
                    ),
                    null
                )
            }
        }

        addPermissionCard(root, "🖼️ 屏幕截图权限", "用于图像识别定位", true) {
            requestScreenCapture()
        }

        // 功能开关区
        root.addView(TextView(this).apply {
            text = "\n功能设置"
            textSize = 18f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(0xFF212121.toInt())
            setPadding(0, 8, 0, 8)
        })

        featureSwitches = mutableMapOf()
        val features = listOf(
            "resource" to "💰 城内收资源",
            "mail" to "📧 自动领邮件",
            "tasks" to "📋 每日任务领取",
            "gather" to "⛏️ 野外资源采集",
            "upgrade" to "🏗️ 自动升级建筑",
            "hunt" to "🐾 自动扫野",
            "alliance" to "🤝 联盟互助",
            "shield" to "🛡️ 自动护盾",
        )

        for ((key, label) in features) {
            val card = android.widget.LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(20, 14, 16, 14)
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0xFFFFFFFF.toInt())
                    cornerRadius = 12f
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, 0, 0, 8) }
            }

            card.addView(TextView(this).apply {
                text = label
                textSize = 15f
                setTextColor(0xFF212121.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
            })

            val switch = SwitchMaterial(this).apply {
                isChecked = key in listOf("resource", "mail", "tasks")
            }
            featureSwitches[key] = switch
            card.addView(switch)
            root.addView(card)
        }

        // 说明
        root.addView(TextView(this).apply {
            text = "\n⚠️ 提示：请遵守游戏规则，合理使用辅助工具。" +
                    "\n建议仅用于收取资源等轻度操作，避免高风险功能。"
            textSize = 12f
            setTextColor(0xFFF44336.toInt())
            setPadding(0, 12, 0, 16)
        })

        // 启动按钮
        startBtn = MaterialButton(this).apply {
            text = "▶ 开始挂机"
            textSize = 18f
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                56
            )
            setBackgroundColor(0xFFE53935.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener { toggleBot() }
        }
        root.addView(startBtn)

        setContentView(scrollView)
    }

    private fun addPermissionCard(
        root: LinearLayout,
        title: String,
        desc: String,
        isScreenshot: Boolean,
        onClick: () -> Unit
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 16)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFFFFFFFF.toInt())
                cornerRadius = 16f
                setStroke(2, 0xFFE0E0E0.toInt())
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 0, 0, 10) }
            setOnClickListener { onClick() }
        }

        card.addView(TextView(this).apply {
            text = title
            textSize = 16f
            setTextColor(0xFF212121.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        card.addView(TextView(this).apply {
            text = desc
            textSize = 13f
            setTextColor(0xFF9E9E9E.toInt())
            setPadding(0, 4, 0, 0)
        })

        root.addView(card)
    }

    private fun checkPermissions() {
        // 检查悬浮窗
        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else true
        if (!hasOverlay) {
            Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestScreenCapture() {
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mpm.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_MEDIA_PROJECTION && resultCode == Activity.RESULT_OK && data != null) {
            // 初始化截屏服务
            val capturer = ScreenCapturer(this)
            if (capturer.start(resultCode, data)) {
                screenCapturer = capturer
                // 把截屏服务传给无障碍服务
                serviceBinder?.setScreenCapturer(capturer)
                serviceBinder?.updateScreenSize(capturer.getWidth(), capturer.getHeight())
                Toast.makeText(this, "截屏权限已开启 ✅", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "截屏初始化失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bindService() {
        val intent = Intent(this, GameAutomationService::class.java)
        startService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun updateUIState() {
        val isRunning = serviceBinder?.isRunning() == true
        statusText.text = if (isRunning) "● 挂机中..." else "● 已停止"
        statusText.setTextColor(
            if (isRunning) 0xFF4CAF50.toInt() else 0xFF9E9E9E.toInt()
        )
        startBtn.text = if (isRunning) "⏹ 停止挂机" else "▶ 开始挂机"
    }

    private fun toggleBot() {
        val isRunning = serviceBinder?.isRunning() == true
        if (isRunning) {
            serviceBinder?.stopBot()
        } else {
            // 检查截屏服务
            if (serviceBinder?.hasScreenCapture() != true) {
                if (screenCapturer != null) {
                    serviceBinder?.setScreenCapturer(screenCapturer!!)
                } else {
                    Toast.makeText(this, "请先开启屏幕截图权限", Toast.LENGTH_SHORT).show()
                    requestScreenCapture()
                    return
                }
            }

            // 收集开关
            val config = mutableMapOf<String, Boolean>()
            for ((key, sw) in featureSwitches) {
                config[key] = sw.isChecked
            }

            // 确保有开启的功能
            if (config.values.none { it }) {
                Toast.makeText(this, "请至少选择一个功能", Toast.LENGTH_SHORT).show()
                return
            }

            serviceBinder?.startBot(config)

            // 启动悬浮窗（如果没有权限先请求）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "请授予悬浮窗权限", Toast.LENGTH_SHORT).show()
                    return
                }
            }
        }
        updateUIState()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceConnected) {
            unbindService(connection)
        }
    }
}
