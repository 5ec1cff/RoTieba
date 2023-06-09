package io.github.a13e300.ro_tieba.misc

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.style.ReplacementSpan
import io.github.a13e300.ro_tieba.R
import kotlin.math.roundToInt

class RoundSpan(
    val context: Context,
    private val bgColor: Int,
    private val textColor: Int,
    private val showText: String? = null,
    private val width: Float? = null,
    private val padding: Float = context.resources.getDimension(R.dimen.round_span_default_padding)
) : ReplacementSpan() {
    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fontMertics: Paint.FontMetricsInt?
    ): Int {
        fontMertics?.apply {
            val pm = paint.fontMetrics
            top = pm.top.toInt()
            ascent = pm.ascent.toInt()
            descent = pm.descent.toInt()
            bottom = pm.bottom.toInt()
        }
        return measureText(paint, text, start, end).roundToInt()
    }

    private fun measureText(paint: Paint, text: CharSequence, start: Int, end: Int): Float {
        val textSize = width
            ?: showText?.let { paint.measureText(showText) }
            ?: paint.measureText(text, start, end)
        return textSize + padding.times(2)
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
        if (showText != null) canvas.drawText(showText, textX, textY, paint)
        else canvas.drawText(text, start, end, textX, textY, paint)
    }
}