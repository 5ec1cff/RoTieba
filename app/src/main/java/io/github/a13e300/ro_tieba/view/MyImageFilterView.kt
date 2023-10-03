package io.github.a13e300.ro_tieba.view

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import androidx.constraintlayout.utils.widget.ImageFilterView
import io.github.a13e300.ro_tieba.R

class MyImageFilterView : ImageFilterView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val ta = context.obtainStyledAttributes(intArrayOf(androidx.appcompat.R.attr.isLightTheme))
        val isLight = ta.getBoolean(0, false)
        ta.recycle()
        if (!isLight) {
            val fg = foreground
            val layer =
                LayerDrawable(arrayOf(ColorDrawable(context.getColor(R.color.image_shadow))))
            if (fg != null) {
                layer.addLayer(fg)
            }
            foreground = layer
        }
    }
}
