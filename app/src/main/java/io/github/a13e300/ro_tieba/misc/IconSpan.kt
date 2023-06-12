package io.github.a13e300.ro_tieba.misc

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.style.ReplacementSpan
import kotlin.math.roundToInt

class IconSpan(private val mDrawable: Drawable) : ReplacementSpan() {
    init {
        mDrawable.setBounds(0, 0, mDrawable.intrinsicWidth, mDrawable.intrinsicHeight)
    }

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fontMetrics: Paint.FontMetricsInt?
    ): Int {
        fontMetrics?.apply {
            val pm = paint.fontMetrics
            top = pm.top.toInt()
            ascent = pm.ascent.toInt()
            descent = pm.descent.toInt()
            bottom = pm.bottom.toInt()
        }
        return paint.textSize.roundToInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val center = y.toFloat() + (paint.fontMetrics.descent + paint.fontMetrics.ascent) / 2
        val size = paint.textSize
        val scaleX = size.div(mDrawable.intrinsicWidth)
        val scaleY = size.div(mDrawable.intrinsicHeight)
        canvas.save()
        canvas.translate(x, center - size / 2)
        canvas.scale(scaleX, scaleY)
        mDrawable.draw(canvas)
        canvas.restore()
    }
}