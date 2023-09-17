package io.github.a13e300.ro_tieba.misc

import android.view.View

fun interface OnPreImeBackPressedListener {
    fun onBackPressed(view: View): Boolean
}