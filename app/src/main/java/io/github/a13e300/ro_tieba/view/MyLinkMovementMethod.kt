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

    data class LastClick(
        val span: URLSpan
    )

    override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x - widget.totalPaddingLeft + widget.scrollX
                val y = event.y.toInt() - widget.totalPaddingTop + widget.scrollY
                val layout = widget.layout
                val line = layout.getLineForVertical(y)
                if (x > layout.getLineWidth(line)) return false
                val off = layout.getOffsetForHorizontal(line, x)
                val span =
                    buffer.getSpans(off, off, URLSpan::class.java).firstOrNull() ?: return false
                if (span is MyURLSpan) span.pressed = true
                widget.invalidate()
                widget.setTag(R.id.tag_movement_method_last_click, LastClick(span))
                val checkForLongClick = CheckForLongClick(widget, event.x, event.y)
                widget.postDelayed(checkForLongClick, LONG_PRESS_THRESHOLD)
                widget.setTag(R.id.tag_movement_method_longclick, checkForLongClick)
                var parent = widget.parent
                while (parent != null) {
                    if (parent is ItemView) {
                        parent.setSelectedData(SelectedLink(span.url))
                        break
                    }
                    parent = parent.parent
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                val link = (widget.getTag(R.id.tag_movement_method_last_click) as? LastClick)?.span
                    ?: return false
                if (link is MyURLSpan) link.pressed = false
                widget.invalidate()
                if (event.eventTime - event.downTime < LONG_PRESS_THRESHOLD) {
                    (widget.getTag(R.id.tag_movement_method_longclick) as? CheckForLongClick)?.let {
                        widget.removeCallbacks(it)
                    }
                    link.onClick(widget)
                }
                widget.setTag(R.id.tag_movement_method_last_click, null)
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                val link = (widget.getTag(R.id.tag_movement_method_last_click) as? LastClick)?.span
                    ?: return false
                if (link is MyURLSpan) link.pressed = false
                widget.invalidate()
                (widget.getTag(R.id.tag_movement_method_longclick) as? CheckForLongClick)?.let {
                    widget.removeCallbacks(it)
                }
                widget.setTag(R.id.tag_movement_method_last_click, null)
                return true
            }
        }
        return false
    }
}