package io.github.a13e300.ro_tieba.view

import android.annotation.SuppressLint
import android.content.Context
import android.text.Spanned
import android.text.style.URLSpan
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.misc.MyURLSpan
import io.github.a13e300.ro_tieba.misc.UserSpan
import io.github.a13e300.ro_tieba.utils.setSelectedData

data class SelectedLink(
    val url: String
)

data class SelectedUser(
    val uidOrPortrait: String
)

class ContentTextView : AppCompatTextView {
    companion object {
        private const val LONG_PRESS_THRESHOLD = 200L
    }

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

    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val text = layout.text
        val buffer = text as? Spanned ?: return super.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x - totalPaddingLeft + scrollX
                val y = event.y.toInt() - totalPaddingTop + scrollY
                val line = layout.getLineForVertical(y)
                if (x > layout.getLineWidth(line)) return false
                val off = layout.getOffsetForHorizontal(line, x)
                val span =
                    buffer.getSpans(off, off, URLSpan::class.java).firstOrNull() ?: return false
                if (span is MyURLSpan) span.pressed = true
                invalidate()
                setTag(
                    R.id.tag_movement_method_last_click,
                    LastClick(span)
                )
                val checkForLongClick =
                    CheckForLongClick(this, event.x, event.y)
                postDelayed(checkForLongClick, LONG_PRESS_THRESHOLD)
                setTag(R.id.tag_movement_method_longclick, checkForLongClick)
                if (span is UserSpan) {
                    setSelectedData(SelectedUser(span.uidOrPortrait))
                } else {
                    setSelectedData(SelectedLink(span.url))
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                val link =
                    (getTag(R.id.tag_movement_method_last_click) as? LastClick)?.span
                        ?: return false
                if (link is MyURLSpan) link.pressed = false
                invalidate()
                if (event.eventTime - event.downTime < LONG_PRESS_THRESHOLD) {
                    (getTag(R.id.tag_movement_method_longclick) as? CheckForLongClick)?.let {
                        removeCallbacks(it)
                    }
                    link.onClick(this)
                }
                setTag(R.id.tag_movement_method_last_click, null)
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                val link =
                    (getTag(R.id.tag_movement_method_last_click) as? LastClick)?.span
                        ?: return false
                if (link is MyURLSpan) link.pressed = false
                invalidate()
                (getTag(R.id.tag_movement_method_longclick) as? CheckForLongClick)?.let {
                    removeCallbacks(it)
                }
                setTag(R.id.tag_movement_method_last_click, null)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}