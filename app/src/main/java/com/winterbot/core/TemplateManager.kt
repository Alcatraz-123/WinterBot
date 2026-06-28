package com.winterbot.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * 模板图片管理器
 * 从 assets/templates/ 目录加载所有模板图
 * 按功能分组管理
 */
object TemplateManager {

    private val templates = mutableMapOf<String, Bitmap>()
    private var loaded = false

    /**
     * 加载所有模板图片
     * assets/templates/ 下的 png 文件都会被加载
     */
    fun loadAll(context: Context) {
        if (loaded) return
        try {
            val assetManager = context.assets
            val files = assetManager.list("templates") ?: return
            for (file in files) {
                if (file.endsWith(".png")) {
                    val key = file.removeSuffix(".png")
                    val inputStream = assetManager.open("templates/$file")
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        templates[key] = bitmap
                    }
                    inputStream.close()
                }
            }
            loaded = true
            android.util.Log.d("TemplateManager", "加载了 ${templates.size} 张模板图")
        } catch (e: Exception) {
            android.util.Log.e("TemplateManager", "加载模板失败: ${e.message}")
        }
    }

    /**
     * 获取模板图
     */
    fun get(name: String): Bitmap? = templates[name]

    /**
     * 检查模板是否存在
     */
    fun has(name: String): Boolean = templates.containsKey(name)

    /**
     * 释放所有模板
     */
    fun release() {
        for (bmp in templates.values) {
            bmp.recycle()
        }
        templates.clear()
        loaded = false
    }
}
