package com.winterbot.core

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import java.nio.ByteBuffer

/**
 * 屏幕截图工具
 * 基于 MediaProjection + ImageReader
 * 使用前需通过 MediaProjectionManager 获取授权 intent
 */
class ScreenCapturer(private val context: Context) {

    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var screenWidth = 1080
    private var screenHeight = 1920
    private var screenDensity = 320

    private val handler = Handler(Looper.getMainLooper())

    /**
     * 初始化截图服务
     * @param resultCode onActivityResult 返回的 resultCode
     * @param data onActivityResult 返回的 intent
     */
    fun start(resultCode: Int, data: Intent): Boolean {
        val mpm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                as? MediaProjectionManager ?: return false

        mediaProjection = mpm.getMediaProjection(resultCode, data)

        // 获取屏幕尺寸
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        screenDensity = metrics.densityDpi

        // 创建 ImageReader
        imageReader = ImageReader.newInstance(
            screenWidth, screenHeight, PixelFormat.RGBA_8888, 2
        )

        // 创建 VirtualDisplay
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "WinterBot-Capture",
            screenWidth, screenHeight, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            handler
        )

        return virtualDisplay != null
    }

    /**
     * 截取当前屏幕
     * @return 屏幕截图 Bitmap，失败返回null
     */
    fun capture(): Bitmap? {
        val reader = imageReader ?: return null
        var image: Image? = null

        try {
            // 等待最新帧
            Thread.sleep(100)
            image = reader.acquireLatestImage()
            if (image == null) return null

            val planes = image.planes
            val buffer: ByteBuffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * screenWidth

            val bitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            // 裁剪掉padding
            return if (rowPadding > 0) {
                Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            android.util.Log.e("ScreenCapturer", "截图失败: ${e.message}")
            return null
        } finally {
            image?.close()
        }
    }

    fun getWidth() = screenWidth
    fun getHeight() = screenHeight

    /**
     * 释放资源
     */
    fun release() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.stop()
        mediaProjection = null
    }
}
