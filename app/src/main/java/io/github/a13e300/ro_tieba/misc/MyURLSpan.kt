package io.github.a13e300.ro_tieba.misc

import android.text.TextPaint
import android.text.style.URLSpan

class MyURLSpan(url: String) : URLSpan(url) {
    var pressed = false
    override fun updateDrawState(ds: TextPaint) {
        ds.color = ds.linkColor
        ds.isUnderlineText = pressed
    }
}