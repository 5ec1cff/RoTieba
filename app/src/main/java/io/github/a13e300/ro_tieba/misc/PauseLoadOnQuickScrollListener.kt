package io.github.a13e300.ro_tieba.misc

import androidx.recyclerview.widget.RecyclerView
import com.github.panpf.sketch.request.PauseLoadWhenScrollingDrawableDecodeInterceptor
import io.github.a13e300.ro_tieba.App
import kotlin.math.abs

class PauseLoadOnQuickScrollListener(height: Int = App.instance.resources.displayMetrics.heightPixels) :
    RecyclerView.OnScrollListener() {
    companion object {
        private const val SAMPLING_DURATION = 250
        private const val SPEED_THRESHOLD = 3.5f
    }

    private val speedThreshold = if (height != 0) (height * 1.5f / 1000) else SPEED_THRESHOLD
    private var mStartTime = 0L
    private var mAllowLoad: Boolean = true
    private var mDy = 0
    private var mLastState: Int = RecyclerView.SCROLL_STATE_IDLE
    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)
        if (recyclerView.adapter != null) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                mAllowLoad = true
                if (PauseLoadWhenScrollingDrawableDecodeInterceptor.scrolling) {
                    PauseLoadWhenScrollingDrawableDecodeInterceptor.scrolling = false
                }
            } else if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
                mAllowLoad = false
                if (!PauseLoadWhenScrollingDrawableDecodeInterceptor.scrolling) {
                    PauseLoadWhenScrollingDrawableDecodeInterceptor.scrolling = true
                }
            }
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                mStartTime = 0L
            } else if (mLastState == RecyclerView.SCROLL_STATE_IDLE) {
                mDy = 0
                mStartTime = System.currentTimeMillis()
            }
            mLastState = newState
        }
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        if (mStartTime != 0L) {
            val dt = System.currentTimeMillis() - mStartTime
            mDy += dy
            if (dt < SAMPLING_DURATION) {
                return
            }
            val speed = abs(mDy.toFloat().div(dt))
            if (mAllowLoad xor (speed < speedThreshold)) {
                mAllowLoad = !mAllowLoad
                if (PauseLoadWhenScrollingDrawableDecodeInterceptor.scrolling == mAllowLoad) {
                    PauseLoadWhenScrollingDrawableDecodeInterceptor.scrolling = !mAllowLoad
                }
            }
        }
        mStartTime = System.currentTimeMillis()
        mDy = 0
    }
}