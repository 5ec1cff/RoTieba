package io.github.a13e300.ro_tieba.view

import android.text.Spannable
import android.text.method.ScrollingMovementMethod
import android.text.style.URLSpan
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.misc.MyURLSpan

data class SelectedLink(
    val url: String
)

object MyLinkMovementMethod : ScrollingMovementMethod() {
    private const val LONG_PRESS_THRESHOLD = 200L

    class CheckForLongClick(
        private val targetView: View,
        private val x: Float, private val y: Float
    ) : Runnable {
        override fun run() {
            targetView.showContextMenu(x, y)
        }
    }

    override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
        val x = event.x - widget.totalPaddingLeft + widget.scrollX
        val y = event.y.toInt() - widget.totalPaddingTop + widget.scrollY
        val layout = widget.layout
        val line = layout.getLineForVertical(y)
        val off = layout.getOffsetForHorizontal(line, x)
        buffer.getSpans(line, off, URLSpan::class.java).firstOrNull()?.let { link ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (link is MyURLSpan) link.pressed = true
                    widget.invalidate()
                    val checkForLongClick = CheckForLongClick(widget, event.x, event.y)
                    widget.postDelayed(checkForLongClick, LONG_PRESS_THRESHOLD)
                    widget.setTag(R.id.tag_last_click_time, checkForLongClick)
                    var parent = widget.parent
                    while (parent != null) {
                        if (parent is ItemView) {
                            parent.setSelectedData(SelectedLink(link.url))
                            break
                        }
                        parent = parent.parent
                    }
                }

                MotionEvent.ACTION_UP -> {
                    if (link is MyURLSpan) link.pressed = false
                    widget.invalidate()
                    if (event.eventTime - event.downTime < LONG_PRESS_THRESHOLD) {
                        (widget.getTag(R.id.tag_last_click_time) as? CheckForLongClick)?.let {
                            widget.removeCallbacks(it)
                        }
                        link.onClick(widget)
                    }
                }

                MotionEvent.ACTION_CANCEL -> {
                    if (link is MyURLSpan) link.pressed = false
                    widget.invalidate()
                    (widget.getTag(R.id.tag_last_click_time) as? CheckForLongClick)?.let {
                        widget.removeCallbacks(it)
                    }
                }
            }
            return super.onTouchEvent(widget, buffer, event)
        }
        return false // super.onTouchEvent(widget, buffer, event) // .also { Logger.d("handled=$it event=$event") }
    }
}