package io.github.a13e300.ro_tieba.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import io.github.a13e300.ro_tieba.Logger
import io.github.a13e300.ro_tieba.R

class ShadowView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val paint = Paint()
    private val mShadowDrawable: Drawable =
        AppCompatResources.getDrawable(context, R.drawable.bottom_background)!!

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(widthMeasureSpec + paddingBottom, heightMeasureSpec + paddingBottom)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val visibleHeight = height - paddingBottom
        val visibleHeightF = visibleHeight.toFloat()
        Logger.d("visibleHeight=$visibleHeight paddingBottom=$paddingBottom")
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        canvas.drawRect(0f, visibleHeightF, width.toFloat(), visibleHeightF + paddingBottom, paint)
        mShadowDrawable.setBounds(0, 0, width, visibleHeight)
        mShadowDrawable.draw(canvas)
    }
}