package com.winterbot.core

import android.graphics.Bitmap
import android.graphics.Rect
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc

/**
 * 图像模板匹配工具
 * 基于 OpenCV TM_CCOEFF_NORMED 算法
 * 匹配度 > threshold 视为找到
 */
object ImageMatcher {

    private var initialized = false

    fun init() {
        if (!initialized) {
            System.loadLibrary("opencv_java4")
            initialized = true
        }
    }

    /**
     * 在屏幕截图中查找模板图片位置
     * @param screen 屏幕截图
     * @param template 模板图（从assets读取）
     * @param threshold 匹配阈值 0.0~1.0，默认0.8
     * @return 匹配到的矩形区域，未找到返回null
     */
    fun findTemplate(screen: Bitmap, template: Bitmap, threshold: Double = 0.8): Rect? {
        if (screen.isRecycled || template.isRecycled) return null

        val screenMat = Mat()
        val templateMat = Mat()
        val resultMat = Mat()

        try {
            Utils.bitmapToMat(screen, screenMat)
            Utils.bitmapToMat(template, templateMat)

            // 转灰度减少计算量
            val grayScreen = Mat()
            val grayTemplate = Mat()
            Imgproc.cvtColor(screenMat, grayScreen, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.cvtColor(templateMat, grayTemplate, Imgproc.COLOR_RGBA2GRAY)

            // 模板匹配
            Imgproc.matchTemplate(grayScreen, grayTemplate, resultMat, Imgproc.TM_CCOEFF_NORMED)

            // 找最大值位置
            val mmr = Core.minMaxLoc(resultMat)
            val maxVal = mmr.maxVal
            val maxLoc: Point = mmr.maxLoc

            if (maxVal >= threshold) {
                val left = maxLoc.x.toInt()
                val top = maxLoc.y.toInt()
                val w = template.width
                val h = template.height
                return Rect(left, top, left + w, top + h)
            }
            return null
        } finally {
            screenMat.release()
            templateMat.release()
            resultMat.release()
        }
    }

    /**
     * 在指定区域内查找模板
     */
    fun findTemplateInRegion(
        screen: Bitmap, template: Bitmap, region: Rect, threshold: Double = 0.8
    ): Rect? {
        if (region.left < 0 || region.top < 0 ||
            region.right > screen.width || region.bottom > screen.height) return null

        val cropped = Bitmap.createBitmap(
            screen, region.left, region.top,
            region.width(), region.height()
        )
        val result = findTemplate(cropped, template, threshold)
        cropped.recycle()
        return result?.let {
            Rect(it.left + region.left, it.top + region.top,
                 it.right + region.left, it.bottom + region.top)
        }
    }

    /**
     * 检查模板是否存在（不需要坐标）
     */
    fun hasTemplate(screen: Bitmap, template: Bitmap, threshold: Double = 0.8): Boolean {
        return findTemplate(screen, template, threshold) != null
    }

    /**
     * 获取矩形中心点
     */
    fun Rect.centerX() = (left + right) / 2
    fun Rect.centerY() = (top + bottom) / 2

    /**
     * 多模板匹配，返回所有匹配点
     */
    fun findAllTemplates(
        screen: Bitmap, template: Bitmap, threshold: Double = 0.8
    ): List<Rect> {
        val results = mutableListOf<Rect>()
        if (screen.isRecycled || template.isRecycled) return results

        val screenMat = Mat()
        val templateMat = Mat()
        val resultMat = Mat()

        try {
            Utils.bitmapToMat(screen, screenMat)
            Utils.bitmapToMat(template, templateMat)

            val grayScreen = Mat()
            val grayTemplate = Mat()
            Imgproc.cvtColor(screenMat, grayScreen, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.cvtColor(templateMat, grayTemplate, Imgproc.COLOR_RGBA2GRAY)

            Imgproc.matchTemplate(grayScreen, grayTemplate, resultMat, Imgproc.TM_CCOEFF_NORMED)

            val w = template.width
            val h = template.height

            // 二值化提取所有高于阈值的点
            val threshMat = Mat()
            Imgproc.threshold(resultMat, threshMat, threshold, 1.0, Imgproc.THRESH_BINARY)
            threshMat.convertTo(threshMat, org.opencv.core.CvType.CV_8UC1)

            // 找轮廓去重
            val contours = mutableListOf<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(threshMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

            for (contour in contours) {
                val rect = Imgproc.boundingRect(contour)
                results.add(Rect(rect.x, rect.y, rect.x + w, rect.y + h))
            }

            threshMat.release()
            hierarchy.release()
        } finally {
            screenMat.release()
            templateMat.release()
            resultMat.release()
        }
        return results
    }

    /**
     * 读取模板图片（从Bitmap）
     */
    fun loadTemplate(bitmap: Bitmap): Bitmap {
        return bitmap.copy(Bitmap.Config.ARGB_8888, false)
    }
}
