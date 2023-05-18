package io.github.a13e300.ro_tieba

import android.annotation.SuppressLint
import android.net.Uri
import android.view.ContextMenu
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.Date

fun ByteArray.toHexString() = this.joinToString("") {
    String.format("%02x", it)
}

fun <T> String.fromJson(klass: Class<T>): T {
    return App.gson.fromJson(this, klass)
}

@SuppressLint("PrivateApi")
private val classMenuBuilder: Class<*>? = runCatching {
    Class.forName("com.android.internal.view.menu.MenuBuilder")
}.onFailure {
    Logger.e("failed to get classMenuBuilder", it)
}.getOrNull()

private val methodSetOptionalIconsVisible: Method? = runCatching {
    classMenuBuilder?.getDeclaredMethod("setOptionalIconsVisible", Boolean::class.java)
        ?.apply { isAccessible = true }
}.onFailure {
    Logger.e("failed to get methodSetOptionalIconsVisible", it)
}.getOrNull()

fun ContextMenu.forceShowIcon() {
    if (classMenuBuilder?.isInstance(this) == true) {
        methodSetOptionalIconsVisible?.invoke(this, true)
    }
}

fun String.convertTiebaUrl(): String {
    if (!startsWith("https://tieba.baidu.com/mo/q/checkurl?url=")) return this
    return Uri.parse(this).getQueryParameter("url") ?: this
}

fun Date.toSimpleString(): String {
    val diff = (System.currentTimeMillis() - time) / 1000
    if (diff >= 0) {
        if (diff < 60) return "${diff}秒前"
        else if (diff < 3600) return "${diff / 60}分钟前"
        else if (diff < 24 * 3600) return "${diff / 3600}小时前"
        else if (diff < 30 * 24 * 3600) return "${diff / (3600 * 24)}天前"
    }
    val ft = SimpleDateFormat("yyyy-MM-dd")
    return ft.format(this)
}
