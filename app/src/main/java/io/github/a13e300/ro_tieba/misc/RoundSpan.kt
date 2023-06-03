package io.github.a13e300.ro_tieba.misc

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.style.ReplacementSpan
import kotlin.math.roundToInt

class RoundSpan(
    private val bgColor: Int,
    private val textColor: Int
) : ReplacementSpan() {
    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        p4: Paint.FontMetricsInt?
    ): Int {
        return measureText(paint, text, start, end).roundToInt()
    }

    private fun measureText(paint: Paint, text: CharSequence, start: Int, end: Int): Float {
        return paint.measureText(text, start, end) + 10
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
        val width = measureText(paint, text, start, end)
        val metrics = paint.fontMetrics
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = bgColor
        val center = y.toFloat() + (metrics.ascent + metrics.descent) / 2
        val fontSize = paint.textSize
        val rectTop = center - fontSize / 2
        val rectBottom = center + fontSize / 2
        val rect = RectF(x, rectTop, x + width, rectBottom)
        canvas.drawRoundRect(rect, 25f, 25f, paint)
        paint.color = Color.BLACK
        paint.textSize = paint.textSize * 0.7f
        paint.textAlign = Paint.Align.CENTER
        paint.color = textColor
        val textMetrics = paint.fontMetrics
        val textX = x + width / 2
        val textY =
            y + (metrics.ascent + metrics.descent - textMetrics.ascent - textMetrics.descent) / 2
        canvas.drawText(text, start, end, textX, textY, paint)
    }
}