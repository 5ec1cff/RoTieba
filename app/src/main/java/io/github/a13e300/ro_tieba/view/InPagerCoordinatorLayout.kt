package io.github.a13e300.ro_tieba.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.coordinatorlayout.widget.CoordinatorLayout
import kotlin.math.abs

class InPagerCoordinatorLayout : CoordinatorLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    private var mStartX = 0f
    private var mStartY = 0f
    private var mDisallow: Boolean? = null

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                mStartX = ev.x
                mStartY = ev.y
                parent.requestDisallowInterceptTouchEvent(true)
                mDisallow = null
            }

            MotionEvent.ACTION_MOVE -> {
                val disallow = mDisallow
                if (disallow != null) {
                    parent.requestDisallowInterceptTouchEvent(disallow)
                } else {
                    val dx = abs(ev.x - mStartX)
                    val dy = abs(ev.y - mStartY)
                    mDisallow = dx < 2 * dy
                    parent.requestDisallowInterceptTouchEvent(dx < 2 * dy)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent.requestDisallowInterceptTouchEvent(true)
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}