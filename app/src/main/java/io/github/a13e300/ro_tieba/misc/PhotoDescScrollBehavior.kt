package io.github.a13e300.ro_tieba.misc

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.FloatProperty
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.widget.NestedScrollView
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.view.BounceScrollView
import kotlin.math.abs
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
    private var mMaxHeightY: Int = 0
    private val mHelper = BounceScrollHelper()
    private var mAnimator: ObjectAnimator? = null
    private var mIsFling: Boolean = false

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
            // content fills the whole space
            if (child.top == mAppBarHeight) {
                consumed[1] = 0
                if (child is BounceScrollView) child.disableBottomBounce = false
                return
            }
            // pre-scroll down if we're below the minHeight line
            if (child.top > mMinHeightY) {
                val consumedY = mHelper.bouncePreScroll(dy)
                child.top = mMinHeightY + mHelper.overScrolledY - dy + consumedY
                consumed[1] = dy
                return
            }
            var consumedY = 0
            // do linear scroll down
            if (child.top > mMaxHeightY) {
                consumedY = dy
                var newTop = child.top - dy
                if (newTop <= mMaxHeightY) {
                    newTop = mMaxHeightY
                    consumedY = child.top - newTop
                    if (mMaxHeightY == mAppBarHeight) {
                        consumed[1] = consumedY
                        child.top = newTop
                        if (child is BounceScrollView) child.disableBottomBounce = false
                        return
                    }
                }
                child.top = newTop
            }
            val unconsumedY = dy - consumedY
            // do bounce scroll down
            if (unconsumedY > 0) {
                mHelper.totalHeight = mMaxHeightY - mAppBarHeight
                if (mIsFling && abs(mHelper.overScrolledY.div(mHelper.totalHeight.toFloat())) > 0.05) {
                    if (child is BounceScrollView) child.disableBottomBounce = true
                    consumed[1] = consumedY
                    return
                }
                mHelper.bounceScroll(unconsumedY)
                child.top = mMaxHeightY + mHelper.overScrolledY
            }
            consumed[1] = dy
        } else if (dy < 0 && child.top < mMaxHeightY) {
            // pre-scroll down if we're above the maxHeight line
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
            if (mIsFling && abs(mHelper.overScrolledY.div(mHelper.totalHeight.toFloat())) > 0.05) return
            var consumedY = 0
            // linear scroll up
            if (child.top < mMinHeightY) {
                consumedY = dyUnconsumed
                var newTop = child.top - dyUnconsumed
                if (newTop >= mMinHeightY) {
                    newTop = mMinHeightY
                    consumedY = child.top - newTop
                }
                child.top = newTop
            }
            // bounce scroll up
            val unconsumedY = dyUnconsumed - consumedY
            if (unconsumedY < 0) {
                mHelper.totalHeight = mMinHeight
                mHelper.bounceScroll(unconsumedY)
                child.top = mMinHeightY + mHelper.overScrolledY
            }
            consumed[1] = dyUnconsumed
        }
    }

    override fun onStopNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: NestedScrollView,
        target: View,
        type: Int
    ) {
        super.onStopNestedScroll(coordinatorLayout, child, target, type)
        if (mIsFling && type != ViewCompat.TYPE_NON_TOUCH) return
        if (mIsFling) mIsFling = false
        startAutoScroll(child)
    }

    override fun onNestedPreFling(
        coordinatorLayout: CoordinatorLayout,
        child: NestedScrollView,
        target: View,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        mIsFling = !super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY)
        return !mIsFling
    }

    private fun startAutoScroll(view: NestedScrollView) {
        val targetTop = if (view.top < mMaxHeightY) mMaxHeightY
        else if (view.top > mMinHeightY) mMinHeightY
        else return
        val height = view.top - targetTop
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
            }
        }
    }
}