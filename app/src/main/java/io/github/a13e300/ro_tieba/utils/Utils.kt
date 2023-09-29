package io.github.a13e300.ro_tieba.utils

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.view.ContextMenu
import android.view.View
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.BuildConfig
import io.github.a13e300.ro_tieba.EXTRA_DONT_USE_NAV
import io.github.a13e300.ro_tieba.Emotions
import io.github.a13e300.ro_tieba.Logger
import io.github.a13e300.ro_tieba.api.json.UserPostResponse
import io.github.a13e300.ro_tieba.misc.EmojiSpan
import io.github.a13e300.ro_tieba.misc.MyURLSpan
import io.github.a13e300.ro_tieba.misc.UserSpan
import io.github.a13e300.ro_tieba.models.Content
import io.github.a13e300.ro_tieba.models.UserProfile
import io.github.a13e300.ro_tieba.view.ItemView
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
    return map {
        // https://github.com/Starry-OvO/aiotieba/blob/ed8867f6ac73b523389dd1dcbdd4b5f62a16ff81/aiotieba/api/get_posts/_classdef.py
        when (it.type) {
            0, // plain text
            9, // phone number
            18, // hash tag
            27, // keyword?
            -> Content.TextContent(it.text)

            1 -> Content.LinkContent(it.text, it.link.convertTiebaUrl())
            4 -> Content.UserContent(it.text, it.uid)

            2, 11 -> Content.EmojiContent(it.text)

            3, 20 -> {
                imageOrder += 1
                val sizes = it.bsize.split(",")
                Content.ImageContent(
                    it.cdnSrc.ifEmpty { it.originSrc },
                    it.originSrc,
                    sizes[0].toInt(),
                    sizes[1].toInt(),
                    imageOrder
                )
            }

            5 -> Content.VideoContent(
                src = Uri.parse(it.link).buildUpon().scheme("https").build().toString(),
                previewSrc = it.src,
                width = it.width,
                height = it.height,
                duration = it.duringTime,
                size = it.originSize,
                text = it.text
            )

            else -> Content.UnknownContent(it.type, it.text, it.toString())
        }
    }
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
            is Content.TextContent -> appendTextAutoLink(content.text)
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
                        content.text.ifEmpty { "UID:${content.uid}" },
                        UserSpan(content.uid),
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
