package io.github.a13e300.ro_tieba.misc

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.OverScroller
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.widget.NestedScrollView
import io.github.a13e300.ro_tieba.R
import kotlin.math.min

val NestedScrollView.contentHeight: Int
    get() = paddingBottom + paddingTop + (getChildAt(0)?.height ?: 0)

class PhotoDescScrollBehavior(context: Context, attrs: AttributeSet) :
    CoordinatorLayout.Behavior<NestedScrollView>(context, attrs) {
    private var mAppBarHeight: Int = 0
    private var mPeekHeight: Int = 0
    private var mMinHeight: Int = 0
    private var mMinHeightY: Int = 0
    private var mScroller: OverScroller? = null

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
        }
        ViewCompat.offsetTopAndBottom(child, mMinHeightY)
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
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
        stopAutoScroll()
        if (dy > 0) {
            val oldTransY = child.translationY
            val newTransY = oldTransY - dy
            val newY = newTransY + child.top
            if (newY >= mAppBarHeight) {
                consumed[1] = dy
                if (newY < coordinatorLayout.height - child.contentHeight)
                    child.translationY = oldTransY - dy / 2
                else
                    child.translationY = newTransY
            } else {
                val realDy = -(oldTransY + child.top - mAppBarHeight).toInt()
                consumed[1] = realDy
                child.translationY = oldTransY + realDy
            }
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
        super.onNestedScroll(
            coordinatorLayout,
            child,
            target,
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            type,
            consumed
        )
        stopAutoScroll()
        if (dyUnconsumed < 0) {
            val newTransY = child.translationY - dyUnconsumed
            val newY = child.top + newTransY
            consumed[1] = dyUnconsumed
            if (newY < mMinHeightY) {
                child.translationY = newTransY
            } else {
                child.translationY = newTransY + dyUnconsumed / 2
            }
        }
    }

    override fun onStopNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: NestedScrollView,
        target: View,
        type: Int
    ) {
        super.onStopNestedScroll(coordinatorLayout, child, target, type)
        val maxY = coordinatorLayout.height - child.contentHeight
        if (child.y < maxY) {
            startAutoScroll(child, (maxY - child.y).toInt())
        } else if (child.y > mMinHeightY) {
            startAutoScroll(child, (mMinHeightY - child.y).toInt())
        }
    }

    private fun startAutoScroll(view: NestedScrollView, distance: Int) {
        val scroller = mScroller ?: OverScroller(view.context).also { mScroller = it }
        if (scroller.isFinished) {
            scroller.startScroll(0, view.translationY.toInt(), 0, distance)
            ViewCompat.postOnAnimation(view, object : Runnable {
                override fun run() {
                    if (scroller.computeScrollOffset()) {
                        view.translationY = scroller.currY.toFloat()
                        ViewCompat.postOnAnimation(view, this)
                    }
                }
            })
        }
    }

    private fun stopAutoScroll() {
        mScroller?.let {
            if (!it.isFinished) {
                it.abortAnimation()
            }
        }
    }
}