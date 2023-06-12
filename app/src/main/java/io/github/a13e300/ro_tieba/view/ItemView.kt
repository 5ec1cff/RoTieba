package io.github.a13e300.ro_tieba.view

import android.content.Context
import android.util.AttributeSet
import android.view.ContextMenu
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import java.lang.ref.WeakReference

class ItemView : FrameLayout {
    data class ContextMenuInfo(
        val data: Any?,
        val targetView: View,
        val selectedData: Any? = null
    ) : ContextMenu.ContextMenuInfo

    private var mData: WeakReference<Any>? = null
    private var mSelectedData: WeakReference<Any>? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    fun <T> setData(data: T?) {
        mData = WeakReference(data)
    }

    fun <T> getData(): T? {
        return mData?.get() as T?
    }

    fun <T> setSelectedData(data: T?) {
        mSelectedData = WeakReference(data)
    }

    fun <T> getSelectedData(): T? {
        return mSelectedData?.get() as T?
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            mSelectedData = null
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun getContextMenuInfo(): ContextMenu.ContextMenuInfo {
        return ContextMenuInfo(mData?.get(), this, mSelectedData?.get())
    }
}
