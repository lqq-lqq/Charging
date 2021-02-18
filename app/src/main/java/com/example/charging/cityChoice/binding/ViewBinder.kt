package com.example.charging.cityChoice.binding

import android.app.Activity
import android.view.View

object ViewBinder {
    fun bind(activity: Activity) {
        com.example.charging.cityChoice.binding.ViewBinder.bind(activity, activity.window.decorView)
    }

    fun bind(target: Any, source: View) {
        val fields = target.javaClass.declaredFields
        if (fields != null && fields.size > 0) {
            for (field in fields) {
                try {
                    field.isAccessible = true
                    if (field[target] != null) {
                        continue
                    }
                    val bind = field.getAnnotation(Bind::class.java)
                    if (bind != null) {
                        val viewId: Int = bind.value
                        field[target] = source.findViewById(viewId)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}