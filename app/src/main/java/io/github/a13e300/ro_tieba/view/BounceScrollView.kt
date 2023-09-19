package io.github.a13e300.ro_tieba.view

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import androidx.core.view.ViewCompat
import androidx.core.widget.NestedScrollView
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.misc.BounceScrollHelper
import kotlin.math.abs

// https://github.com/woxingxiao/BounceScrollView
class BounceScrollView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    NestedScrollView(context, attrs, defStyleAttr) {
    private val mHelper: BounceScrollHelper
    var disableBounce: Boolean
    var disableTopBounce: Boolean = false
    var disableBottomBounce: Boolean = false
    private var mIsFling: Boolean = false
    private var mAnimator: ObjectAnimator? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    init {
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_NEVER
        val a = context.obtainStyledAttributes(attrs, R.styleable.BounceScrollView, 0, 0)
        val damping = a.getFloat(R.styleable.BounceScrollView_damping, DEFAULT_DAMPING_COEFFICIENT)
        val bounceDelay =
            a.getInt(R.styleable.BounceScrollView_bounceDelay, DEFAULT_BOUNCE_DELAY.toInt())
                .toLong()
        disableBounce = a.getBoolean(R.styleable.BounceScrollView_disableBounce, false)
        val enable = a.getBoolean(R.styleable.BounceScrollView_nestedScrollingEnabled, true)
        a.recycle()
        mHelper = BounceScrollHelper()
        mHelper.damping = damping
        mHelper.bounceDelay = bounceDelay
        isNestedScrollingEnabled = enable
    }

    private fun cancelAnimator() {
        if (mAnimator != null && mAnimator!!.isRunning) {
            mAnimator!!.cancel()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelAnimator()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mHelper.totalHeight = measuredHeight
    }

    var damping: Float
        get() = mHelper.damping
        set(damping) {
            mHelper.damping = damping
        }
    var bounceDelay: Long
        get() = mHelper.bounceDelay
        set(bounceDelay) {
            if (bounceDelay >= 0) {
                mHelper.bounceDelay = bounceDelay
            }
        }

    override fun stopNestedScroll(type: Int) {
        super.stopNestedScroll(type)
        if (mHelper.overScrolledY == 0 || (mIsFling && type != ViewCompat.TYPE_NON_TOUCH)) return
        if (mIsFling) mIsFling = false
        mAnimator = mHelper.createScrollBackAnimator(getChildAt(0), TRANSLATION_Y)
        mAnimator!!.start()
    }

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean {
        cancelAnimator()
        val scrolled = super.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)
        if (!disableBounce && !scrolled) {
            val consumedY = mHelper.bouncePreScroll(dy)
            getChildAt(0).translationY = mHelper.overScrolledY.toFloat()
            if (consumed != null) {
                consumed[1] = consumedY
            }
            return true
        }
        return scrolled
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int,
        consumed: IntArray
    ) {
        super.dispatchNestedScroll(
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            offsetInWindow,
            type,
            consumed
        )
        if (!disableBounce && consumed[1] == 0) {
            if (disableTopBounce && dyUnconsumed < 0 || disableBottomBounce && dyUnconsumed > 0) return
            if (mIsFling && abs(mHelper.overScrolledY.div(mHelper.totalHeight.toFloat())) > 0.05) return
            consumed[1] = dyUnconsumed
            mHelper.bounceScroll(dyUnconsumed)
            getChildAt(0).translationY = mHelper.overScrolledY.toFloat()
        }
    }

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        mIsFling = !super.dispatchNestedPreFling(velocityX, velocityY)
        return !mIsFling
    }

    companion object {
        private const val DEFAULT_DAMPING_COEFFICIENT = 4.0f
        private const val DEFAULT_BOUNCE_DELAY: Long = 400
    }
}
