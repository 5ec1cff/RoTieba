package io.github.a13e300.ro_tieba.view

import android.content.Context
import android.util.AttributeSet
import android.view.ContextMenu
import android.view.View
import com.google.android.material.card.MaterialCardView
import io.github.a13e300.ro_tieba.R

class ItemView : MaterialCardView {
    data class ContextMenuInfo(
        val data: Any?,
        val targetView: View
    ) : ContextMenu.ContextMenuInfo

    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    fun <T> setData(data: T?) {
        setTag(R.id.tag_recycler_view_item, data)
    }

    fun <T> getData(): T? {
        return getTag(R.id.tag_recycler_view_item) as T?
    }

    override fun getContextMenuInfo(): ContextMenu.ContextMenuInfo {
        return ContextMenuInfo(getTag(R.id.tag_recycler_view_item), this)
    }
}
