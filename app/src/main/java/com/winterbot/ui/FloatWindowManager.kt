package com.winterbot.ui

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.winterbot.R
import kotlin.math.abs

/**
 * 悬浮窗管理器
 * 显示挂机状态、当前执行的任务
 * 可拖动，点击展开/收起
 */
class FloatWindowManager(private val context: Context) {

    private var windowManager: WindowManager? = null
    private var floatView: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var isShowing = false
    private var isExpanded = false

    private var statusText: TextView? = null
    private var titleText: TextView? = null

    fun show() {
        if (isShowing) return
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createFloatView()
        isShowing = true
    }

    fun hide() {
        if (!isShowing) return
        try {
            floatView?.let { windowManager?.removeView(it) }
        } catch (_: Exception) {}
        floatView = null
        isShowing = false
    }

    fun updateStatus(text: String) {
        statusText?.text = text
    }

    private fun createFloatView() {
        val inflater = LayoutInflater.from(context)
        // 代码创建布局（避免R.layout找不到）
        floatView = createFloatLayout()

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 300
        }

        // 拖动逻辑
        setupDrag()

        try {
            windowManager?.addView(floatView, params)
        } catch (e: Exception) {
            android.util.Log.e("FloatWindow", "添加悬浮窗失败: ${e.message}")
        }
    }

    private fun createFloatLayout(): View {
        // 用代码构建一个简单的悬浮球
        val context = context
        val container = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(0xCC000000.toInt())
            setPadding(24, 16, 24, 16)
            // 圆角背景
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xE61A1A2E.toInt())
                cornerRadius = 40f
                setStroke(2, 0xFF4A90D9.toInt())
            }
        }

        titleText = TextView(context).apply {
            text = "❄ 无尽冬日助手"
            textSize = 13f
            setTextColor(0xFF64B5F6.toInt())
            setPadding(0, 0, 0, 4)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        statusText = TextView(context).apply {
            text = "准备就绪"
            textSize = 12f
            setTextColor(0xFFFFFFFF.toInt())
            maxLines = 2
            // 折叠时默认隐藏详情
            visibility = View.GONE
        }

        container.addView(titleText)
        container.addView(statusText)

        return container
    }

    private fun setupDrag() {
        val view = floatView ?: return
        var startX = 0f
        var startY = 0f
        var touchX = 0f
        var touchY = 0f
        var isDragging = false

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params!!.x.toFloat()
                    startY = params!!.y.toFloat()
                    touchX = event.rawX
                    touchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchX
                    val dy = event.rawY - touchY
                    if (abs(dx) > 8 || abs(dy) > 8) {
                        isDragging = true
                        params!!.x = (startX + dx).toInt()
                        params!!.y = (startY + dy).toInt()
                        try {
                            windowManager?.updateViewLayout(view, params)
                        } catch (_: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // 点击：展开/收起
                        toggleExpand()
                    }
                    // 靠边吸附
                    val screenWidth = context.resources.displayMetrics.widthPixels
                    val centerX = params!!.x + view.width / 2
                    params!!.x = if (centerX < screenWidth / 2) 20 else screenWidth - view.width - 20
                    try {
                        windowManager?.updateViewLayout(view, params)
                    } catch (_: Exception) {}
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleExpand() {
        isExpanded = !isExpanded
        statusText?.visibility = if (isExpanded) View.VISIBLE else View.GONE
    }
}
