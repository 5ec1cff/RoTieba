package io.github.a13e300.ro_tieba.misc

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.FloatProperty
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.widget.NestedScrollView
import io.github.a13e300.ro_tieba.Logger
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.view.BounceScrollView
import kotlin.math.max
import kotlin.math.min

val NestedScrollView.contentHeight: Int
    get() = paddingBottom + paddingTop + (getChildAt(0)?.height ?: 0)

class PhotoDescScrollBehavior(context: Context, attrs: AttributeSet) :
    CoordinatorLayout.Behavior<NestedScrollView>(context, attrs) {
    private var mAppBarHeight: Int = 0
    private var mPeekHeight: Int = 0
    private var mMinHeight: Int = 0
    private var mMinHeightY: Int = 0
    private var mMaxHeight: Int = 0
    private var mMaxHeightY: Int = 0
    private val mHelper = BounceScrollHelper()
    private var mAnimator: ObjectAnimator? = null

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.PhotoDescScrollBehavior_layout,
            0,
            0
        ).apply {
            mPeekHeight = getDimension(
                R.styleable.PhotoDescScrollBehavior_layout_behavior_minHeight,
                100f
            ).toInt()
            recycle()
        }
        mHelper.damping = 4f
        mHelper.bounceDelay = 400
    }

    override fun onMeasureChild(
        parent: CoordinatorLayout,
        child: NestedScrollView,
        parentWidthMeasureSpec: Int,
        widthUsed: Int,
        parentHeightMeasureSpec: Int,
        heightUsed: Int
    ): Boolean {
        mAppBarHeight = parent.findViewById<View>(R.id.app_bar).height
        parent.onMeasureChild(
            child,
            parentWidthMeasureSpec,
            widthUsed,
            parentHeightMeasureSpec,
            heightUsed + mAppBarHeight
        )
        return true
    }

    override fun onLayoutChild(
        parent: CoordinatorLayout,
        child: NestedScrollView,
        layoutDirection: Int
    ): Boolean {
        parent.onLayoutChild(child, layoutDirection)
        val h = child.contentHeight
        mMinHeight = min(mPeekHeight, h)
        mMinHeightY = parent.height - mMinHeight
        if (mMinHeightY < mAppBarHeight) {
            mMinHeightY = mAppBarHeight
            mMinHeight = parent.height - mMinHeightY
        }
        mMaxHeightY = max(parent.height - h, mAppBarHeight)
        child.top = mMinHeightY
        child.bottom = parent.bottom
        if (child is BounceScrollView) child.disableTopBounce = true
        Logger.d("h=$h child=${child.getChildAt(0)?.height} parent=${parent.height} appbar=$mAppBarHeight maxHeightY=$mMaxHeightY minHeightY=$mMinHeightY")
        return true
    }

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: NestedScrollView,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        return (axes and ViewCompat.SCROLL_AXIS_VERTICAL) != 0
    }

    override fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout,
        child: NestedScrollView,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int
    ) {
        stopAutoScroll()
        if (dy > 0) {
            if (child.top == mAppBarHeight) {
                consumed[1] = 0
                return
            }
            if (child.top > mMinHeightY) {
                val consumedY = mHelper.bouncePreScroll(dy)
                child.top = mMinHeightY + mHelper.overScrolledY - dy + consumedY
                consumed[1] = dy
                return
            }
            var consumedY = 0
            if (child.top > mMaxHeightY) {
                consumedY = dy
                var newTop = child.top - dy
                if (newTop <= mMaxHeightY) {
                    newTop = mMaxHeightY
                    consumedY = child.top - newTop
                    if (mMaxHeightY == mAppBarHeight) {
                        consumed[1] = consumedY
                        child.top = newTop
                        return
                    }
                }
                child.top = newTop
            }
            val unconsumedY = dy - consumedY
            if (unconsumedY > 0) {
                mHelper.totalHeight = mMaxHeightY - mAppBarHeight
                mHelper.bounceScroll(unconsumedY)
                child.top = mMaxHeightY + mHelper.overScrolledY
                // Logger.d("pre scroll overScrollY=${mHelper.overScrolledY}")
            }
            consumed[1] = dy
        } else if (dy < 0 && child.top < mMaxHeightY) {
            val consumedY = mHelper.bouncePreScroll(dy)
            child.top = mMaxHeightY + mHelper.overScrolledY - dy + consumedY
            consumed[1] = dy
        }
    }

    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: NestedScrollView,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        stopAutoScroll()
        if (dyUnconsumed < 0) {
            var consumedY = 0
            if (child.top < mMinHeightY) {
                consumedY = dyUnconsumed
                var newTop = child.top - dyUnconsumed
                if (newTop >= mMinHeightY) {
                    newTop = mMinHeightY
                    consumedY = child.top - newTop
                }
                child.top = newTop
            }
            val unconsumedY = dyUnconsumed - consumedY
            if (unconsumedY < 0) {
                mHelper.totalHeight = mMinHeight
                mHelper.bounceScroll(unconsumedY)
                child.top = mMinHeightY + mHelper.overScrolledY
            }
            consumed[1] = unconsumedY
        }
    }

    override fun onStopNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: NestedScrollView,
        target: View,
        type: Int
    ) {
        super.onStopNestedScroll(coordinatorLayout, child, target, type)
        startAutoScroll(child)
    }

    private fun startAutoScroll(view: NestedScrollView) {
        val targetTop = if (view.top < mMaxHeightY) mMaxHeightY
        else if (view.top > mMinHeightY) mMinHeightY
        else return
        val height = view.top - targetTop
        Logger.d("targetTop=$targetTop height=$height")
        val property = object : FloatProperty<NestedScrollView>("") {
            override fun get(p0: NestedScrollView): Float {
                return 1f
            }

            override fun setValue(p0: NestedScrollView, p1: Float) {
                p0.top = (targetTop + p1 * height).toInt()
            }
        }
        mAnimator = mHelper.createScrollBackAnimator(view, property)
        mAnimator!!.start()
    }

    private fun stopAutoScroll() {
        mAnimator?.let {
            if (it.isRunning) {
                it.cancel()
                Logger.d("cancelled")
            }
        }
    }
}