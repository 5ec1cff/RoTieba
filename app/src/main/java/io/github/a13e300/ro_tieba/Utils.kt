package io.github.a13e300.ro_tieba

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.view.ContextMenu
import androidx.appcompat.content.res.AppCompatResources
import io.github.a13e300.ro_tieba.misc.EmojiSpan
import io.github.a13e300.ro_tieba.models.Content
import okhttp3.OkHttpClient
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

fun List<PbContentOuterClass.PbContent>.toPostContent(): List<Content> {
    var imageOrder = 0
    return map {
        // https://github.com/Starry-OvO/aiotieba/blob/ed8867f6ac73b523389dd1dcbdd4b5f62a16ff81/aiotieba/api/get_posts/_classdef.py
        when (it.type) {
            0, // plain text
            4, // at
            9, // phone number
            18, // hash tag
            27, // keyword?
            -> Content.TextContent(it.text)

            1 -> Content.LinkContent(it.text, it.link.convertTiebaUrl())

            2, 11 -> Content.EmojiContent(it.text)

            3, 20 -> {
                imageOrder += 1
                val sizes = it.bsize.split(",")
                Content.ImageContent(
                    it.cdnSrc,
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

fun SpannableStringBuilder.appendSimpleContent(
    contents: List<Content>,
    context: Context
): SpannableStringBuilder {
    contents.forEach { content ->
        when (content) {
            is Content.TextContent -> append(content.text)
            is Content.LinkContent -> append(content.link)
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
