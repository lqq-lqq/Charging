package com.example.charging.cityChoice.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*


/**
 * Created by fySpring
 * Date : 2017/5/10
 * To do :读取本地json文件
 */
object JsonReadUtil {
    fun getJsonStr(context: Context, fileName: String?): String {
        val stringBuffer = StringBuilder()
        val assetManager = context.assets
        try {
            val `is` = assetManager.open(fileName!!)
            val br = BufferedReader(InputStreamReader(`is`))
            var str: String? = null
            while (br.readLine().also { str = it } != null) {
                stringBuffer.append(str)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return stringBuffer.toString()
    }

    fun <T> fromJsonFile(context: Context, fileName: String?, clazz: Class<T>?): List<T> {
        val jsonStr = getJsonStr(context, fileName)
        val lst: MutableList<T> = ArrayList()
        val array: JsonArray = JsonParser().parse(jsonStr).getAsJsonArray()
        for (elem in array) {
            lst.add(Gson().fromJson(elem, clazz))
        }
        return lst
    }

    fun <T> fromJsonArray(jsonStr: String?, clazz: Class<T>?): List<T> {
        val lst: MutableList<T> = ArrayList()
        try {
            val array: JsonArray = JsonParser().parse(jsonStr).getAsJsonArray()
            for (elem in array) {
                lst.add(Gson().fromJson(elem, clazz))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return lst
    }
}
