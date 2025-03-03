package io.github.a13e300.ro_tieba.utils

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.util.TypedValue
import android.view.ContextMenu
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import com.github.panpf.sketch.displayImage
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.request.pauseLoadWhenScrolling
import com.github.panpf.sketch.stateimage.ColorStateImage
import com.github.panpf.sketch.stateimage.IconStateImage
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.BuildConfig
import io.github.a13e300.ro_tieba.EXTRA_DONT_USE_NAV
import io.github.a13e300.ro_tieba.Emotions
import io.github.a13e300.ro_tieba.Logger
import io.github.a13e300.ro_tieba.MobileNavigationDirections
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.api.json.UserPostResponse
import io.github.a13e300.ro_tieba.misc.EmojiSpan
import io.github.a13e300.ro_tieba.misc.MyURLSpan
import io.github.a13e300.ro_tieba.misc.UserSpan
import io.github.a13e300.ro_tieba.models.Content
import io.github.a13e300.ro_tieba.models.PostId
import io.github.a13e300.ro_tieba.models.UserProfile
import io.github.a13e300.ro_tieba.view.ItemView
import io.github.a13e300.ro_tieba.view.MyImageFilterView
import okhttp3.OkHttpClient
import tbclient.MediaOuterClass.Media
import tbclient.PbContentOuterClass
import java.io.InputStream
import java.lang.reflect.Method
import java.net.URLConnection
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.collections.removeLast as removeLastKt

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
    val uri = Uri.parse(this)
    if (uri.authority == "tieba.baidu.com" && uri.path == "/mo/q/checkurl")
        return uri.getQueryParameter("url") ?: this
    else if (uri.scheme == "tiebaclient" && uri.authority == "swan")
        return uri.getQueryParameter("url") ?: this
    return this
}

fun Date.toSimpleString(): String {
    val diff = (System.currentTimeMillis() - time) / 1000
    if (diff >= 0) {
        if (diff < 10) return "刚刚"
        if (diff < 60) return "${diff}秒前"
        else if (diff < 3600) return "${diff / 60}分钟前"
        else if (diff < 24 * 3600) return "${diff / 3600}小时前"
        else if (diff < 30 * 24 * 3600) return "${diff / (3600 * 24)}天前"
    }
    val ft = SimpleDateFormat("yyyy-MM-dd")
    return ft.format(this)
}

fun List<PbContentOuterClass.PbContent>.toPostContent(): List<Content> {
    var imageOrder = 0
    val result = mutableListOf<Content>()
        // https://github.com/Starry-OvO/aiotieba/blob/ed8867f6ac73b523389dd1dcbdd4b5f62a16ff81/aiotieba/api/get_posts/_classdef.py
    for (item in this) {
        val content = when (item.type) {
            0, // plain text
            9, // phone number
            18, // hash tag
            27, // keyword?
            -> {
                val last = result.lastOrNull() as? Content.TextContent
                if (last == null)
                    Content.TextContent(item.text)
                else {
                    result.removeLastKt()
                    Content.TextContent(last.text + item.text)
                }
            }

            1 -> Content.LinkContent(item.text, item.link.convertTiebaUrl())
            4 -> Content.UserContent(item.text, item.uid.toString())

            2, 11 -> Content.EmojiContent(item.text)

            3, 20 -> {
                imageOrder += 1
                val sizes = item.bsize.split(",")
                Content.ImageContent(
                    item.cdnSrc.ifEmpty { item.originSrc },
                    item.originSrc,
                    sizes[0].toInt(),
                    sizes[1].toInt(),
                    imageOrder
                )
            }

            5 -> Content.VideoContent(
                src = Uri.parse(item.link).buildUpon().scheme("https").build().toString(),
                previewSrc = item.src,
                width = item.width,
                height = item.height,
                duration = item.duringTime,
                size = item.originSize,
                text = item.text
            )

            else -> Content.UnknownContent(item.type, item.text, item.toString())
        }
        result.add(content)
    }
    return result
}

fun List<UserPostResponse.PostContent>.replyContentToPostContent(): List<Content> = map { c ->
    when (c.type) {
        "0" -> {
            Emotions.getEmotionForName("#${c.text}")?.let {
                Content.EmojiContent(it.id)
            } ?: Content.TextContent(c.text)
            // TODO: image & user
        }

        else -> Content.TextContent(c.text)
    }
}

fun SpannableStringBuilder.appendSimpleContent(
    contents: List<Content>,
    context: Context,
    useUrlSpan: Boolean = false
): SpannableStringBuilder {
    contents.forEach { content ->
        when (content) {
            is Content.TextContent -> {
                val settings = App.settings
                if (useUrlSpan)
                    appendTextAutoLink(
                        content.text,
                        !settings.disableAutoLink,
                        !settings.disableAutoBv
                    )
                else
                    append(content.text)
            }
            is Content.LinkContent -> {
                if (useUrlSpan) {
                    append(
                        content.text.ifEmpty { "[link]" },
                        MyURLSpan(content.link),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                } else append(content.text)
            }

            is Content.UserContent -> {
                if (useUrlSpan) {
                    append(
                        content.text.ifEmpty { "UID:${content.uidOrPortrait}" },
                        UserSpan(content.uidOrPortrait),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                } else append(content.text)
            }

            is Content.EmojiContent -> {
                val emoji = Emotions.emotionMap.get(content.id)
                if (emoji == null) {
                    append("[$emoji]")
                } else {
                    val drawable = AppCompatResources.getDrawable(
                        context,
                        emoji.resource
                    )!!
                    append(
                        emoji.name,
                        EmojiSpan(drawable),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            is Content.ImageContent -> append("[图片]")
            else -> {}
        }
    }
    return this
}

@SuppressLint("CustomX509TrustManager")
fun OkHttpClient.Builder.ignoreAllSSLErrors(): OkHttpClient.Builder {
    val naiveTrustManager = object : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) = Unit
        override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) = Unit
    }

    val insecureSocketFactory = SSLContext.getInstance("TLSv1.2").apply {
        val trustAllCerts = arrayOf<TrustManager>(naiveTrustManager)
        init(null, trustAllCerts, SecureRandom())
    }.socketFactory

    sslSocketFactory(insecureSocketFactory, naiveTrustManager)
    hostnameVerifier { _, _ -> true }
    return this
}

fun OkHttpClient.Builder.ignoreAllSSLErrorsIfDebug(): OkHttpClient.Builder =
    if (BuildConfig.DEBUG) ignoreAllSSLErrors()
    else this

fun InputStream.guessExtension(): String {
    val type = URLConnection.guessContentTypeFromStream(this)
    return when (type) {
        "image/png" -> "png"
        "image/gif" -> "gif"
        else -> "jpg"
    }
}

fun openAtOtherClient(uri: Uri, context: Context): Boolean {
    if (context.packageManager.queryIntentActivities(Intent(Intent.ACTION_VIEW, uri), 0)
            .map { it.activityInfo.packageName }.none { it != BuildConfig.APPLICATION_ID }
    ) return false
    context.startActivity(
        Intent(Intent.ACTION_VIEW, uri),
        bundleOf(EXTRA_DONT_USE_NAV to true)
    )
    return true
}

fun openUserAtOtherClient(user: UserProfile, context: Context) =
// com.baidu.tieba://unidispatch/usercenter?portrait={}
    // or com.baidu.tieba://usercenter//uid={}
    openAtOtherClient(
        Uri.Builder()
            .scheme("com.baidu.tieba")
            .authority("unidispatch")
            .appendPath("usercenter")
            .appendQueryParameter("portrait", user.portrait)
            .build(),
        context
    )

fun openForumAtOtherClient(forumName: String, context: Context) =
    openAtOtherClient(
        Uri.Builder()
            .scheme("com.baidu.tieba")
            .authority("unidispatch")
            .appendPath("frs")
            .appendQueryParameter("kw", forumName)
            .build(),
        context
    )

fun openPostAtOtherClient(tid: Long, pid: Long, context: Context) =
    openAtOtherClient(
        Uri.Builder()
            .scheme("com.baidu.tieba")
            .authority("unidispatch")
            .appendPath("pb")
            .appendQueryParameter("tid", tid.toString())
            .apply {
                if (pid != 0L) appendQueryParameter("hightlight_anchor_pid", pid.toString())
            }
            .build(),
        context
    )

fun List<Media>.toImageContentList() = filter { it.type == 3 }.mapIndexed { i, pic ->
    Content.ImageContent(pic.srcPic, pic.originPic, pic.width, pic.height, i)
}

fun Context.copyText(text: CharSequence) {
    getSystemService(ClipboardManager::class.java).setPrimaryClip(
        ClipData.newPlainText("", text)
    )
    Toast.makeText(this, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
}

fun Fragment.copyText(text: CharSequence) {
    requireContext().copyText(text)
}

fun <T> View.setSelectedData(data: T) {
    var parent = this.parent
    while (parent != null && parent !is ItemView) parent = parent.parent
    (parent as? ItemView)?.setSelectedData(data)
}

fun Int.toSimpleString() =
    when (this) {
        in 0 until 10000 -> toString()
        in 10000 until 100000 -> "%.1fW".format(this.toFloat() / 10000)
        else -> "10W+"
    }

fun Long.toSimpleString() =
    when (this) {
        in 0L until 10000L -> toString()
        in 10000L until 100000L -> "%.1fW".format(this.toFloat() / 10000)
        else -> "10W+"
    }

fun Int.dp2px(context: Context) = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this.toFloat(),
    context.resources.displayMetrics
).toInt()

fun ImageView.displayImageInList(
    uri: String,
    configBlock: (DisplayRequest.Builder.() -> Unit)? = null
) = displayImage(uri) {
    pauseLoadWhenScrolling(true)
    configBlock?.invoke(this)
}

fun String.parseThreadLink(): PostId? {
    val uri = Uri.parse(this)
    if ((uri.scheme == "http" || uri.scheme == "https") && uri.host == "tieba.baidu.com"
        && uri.pathSegments.size == 2 && uri.pathSegments[0] == "p"
    ) {
        val tid = uri.pathSegments[1].toLongOrNull() ?: return null
        val pid = uri.getQueryParameter("pid")?.toLongOrNull()
        val cid = uri.getQueryParameter("cid")?.toLongOrNull()
        if (pid != null) {
            if (cid != null) {
                return PostId.Comment(tid, pid, cid)
            } else {
                return PostId.Post(tid, pid)
            }
        } else {
            return PostId.Thread(tid)
        }
    }
    return null
}

fun NavController.navigateToPost(id: PostId) {
    navigate(
        when (id) {
            is PostId.Comment -> MobileNavigationDirections.showComments(
                id.tid,
                id.pid,
                id.spid
            )

            is PostId.Post -> MobileNavigationDirections.goToThread(id.tid)
                .setPid(id.pid)

            is PostId.Thread -> MobileNavigationDirections.goToThread(id.tid)
        }
    )
}

fun MyImageFilterView.configureImageForContent(content: Content.ImageContent) {
    // backend returns max width is 560
    imageScale = content.width.div(560f)
    imageRatio = content.width.toFloat().div(content.height)
}

fun DisplayRequest.Builder.configureDefaults(context: Context) {
    val ta =
        context.obtainStyledAttributes(intArrayOf(com.google.android.material.R.attr.colorSurfaceVariant))
    val color = ta.getColor(0, 0x1a1c1e)
    ta.recycle()
    placeholder(ColorStateImage(color))
    error(IconStateImage(R.drawable.ic_error) { colorBackground(color) })
}

inline fun <T> List<T>.firstOrNullFrom(from: Int = 0, predicate: (T) -> Boolean): T? {
    for (i in from until size) {
        val item = this[i]
        if (predicate(item)) return item
    }
    return null
}

inline fun <T> List<T>.indexOfFrom(from: Int = 0, predicate: (T) -> Boolean): Int {
    for (i in from until size) {
        val item = this[i]
        if (predicate(item)) return i
    }
    return -1
}

const val USER_REGEX =
    "<a .* portrait=\"(?<portrait>.*?)\" target=\"_blank\" class=\"at\"> (?<name>.*?)</a>"
const val EMOTION_REGEX =
    "<img class=\"BDE_Smiley\" .* src=\"http://static\\.tieba\\.baidu\\.com/tb/editor/images/client/(?<emotionname>.*?)\\.png\" >"
const val LINK_REGEX = "<a href=\"(?<url>.*?)\" .*?>(?<linktext>.*?)</a>"
val HTML_REGEX by lazy { Regex("(?<user>$USER_REGEX)|(?<emotion>$EMOTION_REGEX)|(?<link>$LINK_REGEX)") }

fun String.htmlToContent(): List<Content> {
    replace("<br>", "\n").apply {
        val result = mutableListOf<Content>()
        var start = 0
        var matched = HTML_REGEX.find(this, start)
        while (matched != null) {
            val s = matched.range.first
            if (s > start)
                result.add(Content.TextContent(substring(start, s)))
            if (matched.groups["user"] != null) {
                result.add(
                    Content.UserContent(
                        text = matched.groups["name"]!!.value,
                        uidOrPortrait = matched.groups["portrait"]!!.value
                    )
                )
            } else if (matched.groups["emotion"] != null) {
                result.add(Content.EmojiContent(id = matched.groups["emotionname"]!!.value))
            } else if (matched.groups["link"] != null) {
                result.add(
                    Content.LinkContent(
                        text = matched.groups["linktext"]!!.value,
                        link = matched.groups["url"]!!.value
                    )
                )
            }
            start = matched.range.last + 1
            matched = HTML_REGEX.find(this, start)
        }
        if (start < length) result.add(Content.TextContent(substring(start, length)))
        return result
    }
}
