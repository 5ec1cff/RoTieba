package io.github.a13e300.ro_tieba.view

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import com.google.android.material.search.SearchView
import io.github.a13e300.ro_tieba.misc.OnPreImeBackPressedListener

class MySearchView : SearchView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    override fun dispatchKeyEventPreIme(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_BACK &&
            onBackPressedListener?.onBackPressed(this) == true
        ) {
            return true
        }
        return super.dispatchKeyEventPreIme(event)
    }

    var onBackPressedListener: OnPreImeBackPressedListener? = null
}