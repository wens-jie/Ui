package com.wens.ui.util

import android.content.Context
import android.view.WindowManager


class ScreenInfo {
    companion object {
        fun height(context: Context): Int =
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).currentWindowMetrics.bounds.height()

        fun width(context: Context): Int =
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).currentWindowMetrics.bounds.width()

        /**
         * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
         */
        fun dp2px(context: Context, dpValue: Float): Int =
            (dpValue * context.resources.displayMetrics.density + 0.5f).toInt()

        fun dp2px(context: Context, dpValue: Int): Int =
            (dpValue * context.resources.displayMetrics.density + 0.5f).toInt()


        /**
         * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
         */
        fun px2dp(context: Context, pxValue: Float): Int {
            return (pxValue / context.resources.displayMetrics.density + 0.5f).toInt()
        }

        // 将px值转换为sp值
        fun px2sp(context: Context, pxValue: Float): Int {
            return (pxValue / context.resources.displayMetrics.scaledDensity + 0.5f).toInt()
        }

        /**
         * 将sp值转换为px值，保证文字大小不变
         */
        fun sp2px(context: Context, spValue: Float): Int {
            return (spValue * context.resources.displayMetrics.scaledDensity + 0.5f).toInt()
        }
    }
}