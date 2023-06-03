package io.github.a13e300.ro_tieba.view

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import com.google.android.material.search.SearchView

class MySearchView : SearchView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    override fun dispatchKeyEventPreIme(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP && event.keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPressedListener?.onBackPressed(this)
        }
        return super.dispatchKeyEventPreIme(event)
    }

    fun interface OnBackPressedListener {
        fun onBackPressed(view: View)
    }

    var onBackPressedListener: OnBackPressedListener? = null
}