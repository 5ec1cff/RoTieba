package io.github.a13e300.ro_tieba.view

import android.annotation.SuppressLint
import android.content.Context
import android.text.Spanned
import android.text.style.URLSpan
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.misc.MyURLSpan
import io.github.a13e300.ro_tieba.utils.setSelectedData

class ContentTextView : AppCompatTextView {
    companion object {
        private const val LONG_PRESS_THRESHOLD = 200L
    }

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
                    MyLinkMovementMethod.LastClick(span)
                )
                val checkForLongClick =
                    MyLinkMovementMethod.CheckForLongClick(this, event.x, event.y)
                postDelayed(checkForLongClick, LONG_PRESS_THRESHOLD)
                setTag(R.id.tag_movement_method_longclick, checkForLongClick)
                setSelectedData(SelectedLink(span.url))
                return true
            }

            MotionEvent.ACTION_UP -> {
                val link =
                    (getTag(R.id.tag_movement_method_last_click) as? MyLinkMovementMethod.LastClick)?.span
                        ?: return false
                if (link is MyURLSpan) link.pressed = false
                invalidate()
                if (event.eventTime - event.downTime < LONG_PRESS_THRESHOLD) {
                    (getTag(R.id.tag_movement_method_longclick) as? MyLinkMovementMethod.CheckForLongClick)?.let {
                        removeCallbacks(it)
                    }
                    link.onClick(this)
                }
                setTag(R.id.tag_movement_method_last_click, null)
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                val link =
                    (getTag(R.id.tag_movement_method_last_click) as? MyLinkMovementMethod.LastClick)?.span
                        ?: return false
                if (link is MyURLSpan) link.pressed = false
                invalidate()
                (getTag(R.id.tag_movement_method_longclick) as? MyLinkMovementMethod.CheckForLongClick)?.let {
                    removeCallbacks(it)
                }
                setTag(R.id.tag_movement_method_last_click, null)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}