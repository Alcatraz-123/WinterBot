package com.winterbot

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textview.MaterialTextView

class MainActivity : AppCompatActivity() {

    private var serviceBinder: GameAutomationService.LocalBinder? = null
    private var serviceConnected = false

    private lateinit var startBtn: MaterialButton
    private lateinit var statusText: MaterialTextView
    private lateinit var featureSwitches: MutableMap<String, SwitchMaterial>

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
        setupUI()
        bindService()
    }

    private fun setupUI() {
        // 线性布局，方便动态控制
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, android.R.color.background_light))
        }

        // 标题
        root.addView(TextView(this).apply {
            text = "🔥 无尽冬日助手"
            textSize = 26f
            setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.black))
            setPadding(0, 0, 0, 16)
        })

        root.addView(TextView(this).apply {
            text = "打开上方开关 → 启动无障碍服务 → 开始挂机"
            textSize = 14f
            alpha = 0.6f
            setPadding(0, 0, 0, 24)
        })

        // 状态指示
        statusText = MaterialTextView(this).apply {
            text = "● 服务未启动"
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.darker_gray))
            setPadding(0, 0, 0, 16)
        }
        root.addView(statusText)

        // 无障碍服务开关引导
        val accessibilityCard = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 0, 0, 12) }
            radius = 16f
            setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
            elevation = 4f
        }

        val accessibilityRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 16, 20, 16)
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        accessibilityRow.addView(TextView(this).apply {
            text = "🔓 无障碍服务"
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        accessibilityRow.addView(TextView(this).apply {
            text = "去开启 ›"
            textSize = 14f
            setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_light))
        })

        accessibilityCard.addView(accessibilityRow)
        root.addView(accessibilityCard)

        // 功能开关列表
        root.addView(TextView(this).apply {
            text = "\n功能设置"
            textSize = 18f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 8, 0, 8)
        })

        featureSwitches = mutableMapOf()
        val features = listOf(
            "gather" to "💰 自动收取资源",
            "upgrade" to "🏗️ 自动升级建筑",
            "hunt" to "🐾 自动扫野",
            "alliance" to "🤝 联盟互助",
            "shield" to "🛡️ 自动护盾",
            "tasks" to "📋 每日任务",
            "mail" to "📧 自动领邮件",
        )

        for ((key, label) in features) {
            val card = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, 0, 0, 8) }
                radius = 12f
                setCardBackgroundColor(ContextCompat.getColor(this@MainActivity, android.R.color.white))
                elevation = 2f
            }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 8, 8, 8)
            }

            row.addView(TextView(this).apply {
                text = label
                textSize = 15f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            val switch = SwitchMaterial(this).apply {
                isChecked = true
            }
            featureSwitches[key] = switch
            row.addView(switch)
            card.addView(row)
            root.addView(card)
        }

        // 按钮区
        root.addView(TextView(this).apply {
            text = "\n"
            textSize = 8f
        })

        startBtn = MaterialButton(this).apply {
            text = "▶ 开始挂机"
            textSize = 18f
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                56
            )
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_light))
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener { toggleBot() }
        }
        root.addView(startBtn)

        setContentView(root)
    }

    private fun bindService() {
        val intent = Intent(this, GameAutomationService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun updateUIState() {
        val isRunning = serviceBinder?.isRunning() == true
        statusText.text = if (isRunning) "● 挂机中..." else "● 已停止"
        statusText.setTextColor(
            ContextCompat.getColor(
                this,
                if (isRunning) android.R.color.holo_green_dark else android.R.color.darker_gray
            )
        )
        startBtn.text = if (isRunning) "⏹ 停止挂机" else "▶ 开始挂机"
    }

    private fun toggleBot() {
        val isRunning = serviceBinder?.isRunning() == true
        if (isRunning) {
            serviceBinder?.stopBot()
        } else {
            // 收集开关状态
            val config = mutableMapOf<String, Boolean>()
            for ((key, sw) in featureSwitches) {
                config[key] = sw.isChecked
            }
            serviceBinder?.startBot(config)
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
