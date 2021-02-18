package com.example.charging.cityChoice.app

import android.app.Application
import com.example.charging.cityChoice.utils.ScreenUtils
import com.example.charging.cityChoice.utils.ToastUtils


/**
 * Created by fySpring
 * Date : 2017/10/9
 * To do :
 */
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        init()
    }

    private fun init() {
        ToastUtils.init(this)
        ScreenUtils.init(this)
    }
}