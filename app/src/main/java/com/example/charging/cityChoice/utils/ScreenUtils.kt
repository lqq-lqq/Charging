package com.example.charging.cityChoice.utils

import android.content.Context
import android.view.WindowManager

object ScreenUtils {  //单例类
    private var sContext: Context? = null
    fun init(context: Context) {
        sContext = context.applicationContext    //百度，是获得application实例
        //println("初始化sContext")
    }

    val screenWidth: Int
        get() {
            val wm =
                sContext!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            return wm.defaultDisplay.width
        }

    val screenHeight: Int
        get() {
            val wm =
                sContext!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            return wm.defaultDisplay.height
        }

    /**
     * 获取状态栏高度
     */
    val systemBarHeight: Int
        get() {
            var result = 0
            //val resourceId=0;
            println("here is the resourceId")
            var resourceId:Int=0
            if(sContext==null){
                //println("sContext is null")
            }
            else{
                //println("sContent is not null")
                resourceId = sContext!!.resources .getIdentifier("status_bar_height", "dimen", "android")  //有问题，sContent一直是null
            }
            //print(sContext!!)
            //resourceId = sContext!!.resources .getIdentifier("status_bar_height", "dimen", "android")  //有问题
            //println("resourceId:"+resourceId)
            if (resourceId > 0) {
                result = sContext!!.resources.getDimensionPixelSize(resourceId)
            }
            return result
        }

    fun dp2px(dpValue: Float): Int {
        val scale = sContext!!.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    fun px2dp(pxValue: Float): Int {
        val scale = sContext!!.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

    fun sp2px(spValue: Float): Int {
        val fontScale =
            sContext!!.resources.displayMetrics.scaledDensity
        return (spValue * fontScale + 0.5f).toInt()
    }
}