package io.github.a13e300.ro_tieba.misc

import androidx.recyclerview.widget.RecyclerView
import com.github.panpf.sketch.request.PauseLoadWhenScrollingDrawableDecodeInterceptor

class PauseLoadOnQuickScrollListener : RecyclerView.OnScrollListener() {
    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)
        if (recyclerView.adapter != null) {
            if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
                // just pause load when settling (flinging) for now
                if (!PauseLoadWhenScrollingDrawableDecodeInterceptor.scrolling) {
                    PauseLoadWhenScrollingDrawableDecodeInterceptor.scrolling = true
                }
            } else {
                if (PauseLoadWhenScrollingDrawableDecodeInterceptor.scrolling) {
                    PauseLoadWhenScrollingDrawableDecodeInterceptor.scrolling = false
                }
            }
        }
    }
}