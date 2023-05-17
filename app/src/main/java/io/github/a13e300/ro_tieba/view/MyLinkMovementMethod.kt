package io.github.a13e300.ro_tieba.view

import android.text.Spannable
import android.text.method.ScrollingMovementMethod
import android.text.style.URLSpan
import android.view.MotionEvent
import android.widget.TextView

data class SelectedLink(
    val url: String
)

object MyLinkMovementMethod : ScrollingMovementMethod() {
    override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
        if (widget is PbContentTextView) {
            val x = event.x - widget.totalPaddingLeft + widget.scrollX
            val y = event.y.toInt() - widget.totalPaddingTop + widget.scrollY
            val layout = widget.layout
            val line = layout.getLineForVertical(y)
            val off = layout.getOffsetForHorizontal(line, x)
            val link = buffer.getSpans(line, off, URLSpan::class.java).firstOrNull()
                ?: return super.onTouchEvent(widget, buffer, event)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    var parent = widget.parent
                    while (parent != null) {
                        if (parent is ItemView) {
                            parent.setSelectedData(SelectedLink(link.url))
                            break
                        }
                        parent = parent.parent
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!widget.longClicked) {
                        link.onClick(widget)
                    }
                }
            }
        }
        return super.onTouchEvent(widget, buffer, event)
    }
}