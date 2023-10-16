package io.github.a13e300.ro_tieba.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.constraintlayout.utils.widget.ImageFilterView
import io.github.a13e300.ro_tieba.R

class MyImageFilterView : ImageFilterView {
    // w / h
    var imageRatio: Float = 0f
    var imageScale: Float = 0f
    private var needMask = false
    private lateinit var mMaskPaint: Paint

    constructor(context: Context) : super(context) {
        init()
    }
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    private fun init() {
        val ta = context.obtainStyledAttributes(intArrayOf(androidx.appcompat.R.attr.isLightTheme))
        needMask = !ta.getBoolean(0, false)
        if (needMask) mMaskPaint = Paint().apply {
            color = context.getColor(R.color.image_shadow)
            style = Paint.Style.FILL
        }
        ta.recycle()
    }

    override fun onDrawForeground(canvas: Canvas) {
        if (needMask) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), mMaskPaint)
        }
        super.onDrawForeground(canvas)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (imageRatio <= 0 || imageScale <= 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        // assume widthMeasureSpec is EXACTLY
        val maxW = MeasureSpec.getSize(widthMeasureSpec)
        val scale = if (imageScale > 1) 1.0f else imageScale
        val width = maxW.times(scale).toInt()
        val height = maxW.div(imageRatio).times(scale).toInt()
        setMeasuredDimension(width, height)
    }
}
