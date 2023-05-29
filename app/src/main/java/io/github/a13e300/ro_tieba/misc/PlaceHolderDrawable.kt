package io.github.a13e300.ro_tieba.misc

import android.graphics.Color
import android.graphics.drawable.ColorDrawable

class PlaceHolderDrawable(private val width: Int, private val height: Int) :
    ColorDrawable(Color.WHITE) {
    override fun getIntrinsicWidth(): Int {
        return width
    }

    override fun getIntrinsicHeight(): Int {
        return height
    }
}