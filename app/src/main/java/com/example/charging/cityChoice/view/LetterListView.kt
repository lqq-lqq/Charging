package com.example.charging.cityChoice.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.charging.cityChoice.utils.DisplayUtil


class LetterListView : View {
    var onTouchingLetterChangedListener: OnTouchingLetterChangedListener? = null
    var choose = -1
    var paint = Paint()
    var showBkg = false
    private var mContext: Context

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int
    ) : super(context, attrs, defStyle) {
        mContext = context
    }

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context, attrs) {
        mContext = context
    }

    constructor(context: Context) : super(context) {
        mContext = context
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (showBkg) {
            canvas.drawColor(Color.parseColor("#40000000"))
        }
        val height = height
        val width = width
        val singleHeight = height / b.size
        for (i in b.indices) {
            paint.color = Color.parseColor("#50B3DA")
            paint.textSize = DisplayUtil.sp2px(mContext, 12f).toFloat()
            paint.isAntiAlias = true
            val xPos =
                width / 2 - paint.measureText(b[i]) / 2
            val yPos = singleHeight * i + singleHeight.toFloat()
            canvas.drawText(b[i], xPos, yPos, paint)
            paint.reset()
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        val y = event.y
        val oldChoose = choose
        val listener =
            onTouchingLetterChangedListener
        val c = (y / height * b.size).toInt()
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                showBkg = true
                if (oldChoose != c && listener != null) {
                    if (c >= 0 && c < b.size) {
                        listener.onTouchingLetterChanged(b[c])
                        choose = c
                        invalidate()
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> if (oldChoose != c && listener != null) {
                if (c >= 0 && c < b.size) {
                    listener.onTouchingLetterChanged(b[c])
                    choose = c
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                showBkg = false
                choose = -1
                invalidate()
            }
        }
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return super.onTouchEvent(event)
    }


    interface OnTouchingLetterChangedListener {
        fun onTouchingLetterChanged(s: String?)
    }

    companion object {
        var b = arrayOf(
            "定位", "热门", "A", "B", "C", "D", "E", "F", "G", "H", "J", "K",
            "L", "M", "N", "P", "Q", "R", "S", "T", "W", "X", "Y", "Z"
        )
    }
}