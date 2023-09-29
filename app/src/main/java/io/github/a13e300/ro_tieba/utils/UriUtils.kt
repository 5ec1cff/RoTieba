package io.github.a13e300.ro_tieba.utils

import android.text.Spannable
import android.text.SpannableStringBuilder
import io.github.a13e300.ro_tieba.misc.MyURLSpan

const val HOST_CHARS = "a-zA-Z0-9_\\-"
const val HOST_WORD_PATTERN = "[$HOST_CHARS]+"

// prevent from decimals are recognized as url unexpectedly
const val HOST_PATTERN = "(?:$HOST_WORD_PATTERN\\.)+[a-zA-Z]{2,}"

const val URL_PREFIX_PATTERN = "(?:https?://)?$HOST_PATTERN"

const val URL_PATH_CHARS = "!#\$&'*+\\(\\),-./:;=\\?@_~0-9A-Za-z"
const val URL_PATH_PATTERN = "/[$URL_PATH_CHARS]*"

const val URL_PATTERN = "$URL_PREFIX_PATTERN(?:$URL_PATH_PATTERN)?"

// https://www.zhihu.com/question/381784377/answer/1099438784
const val BV_CHARS = "fZodR9XQDSUm21yCkr6zBqiveYah8bt4xsWpHnJE7jL5VG3guMTKNPAwcF"
const val BILIBILI_VIDEO_PATTERN = "av\\d+|BV[$BV_CHARS]{10}"

val URL_REGEX = Regex("(?<url>$URL_PATTERN)")
val URL_OR_VIDEO_REGEX = Regex("(?<url>$URL_PATTERN)|(?<video>$BILIBILI_VIDEO_PATTERN)")

sealed class TextFragment {
    data class Text(val text: String) : TextFragment()
    data class Url(val url: String) : TextFragment()
    data class BilibiliVideo(val id: String) : TextFragment()
}

inline fun CharSequence.mapText(
    parseVideo: Boolean = false,
    callback: (f: TextFragment, start: Int, end: Int) -> Unit
) {
    val regex = if (parseVideo) URL_OR_VIDEO_REGEX else URL_REGEX
    var start = 0
    var matched = regex.find(this, start)
    while (matched != null) {
        val s = matched.range.first
        if (s > start)
            callback(TextFragment.Text(substring(start, s)), start, s)
        val url = matched.groups["url"]
        val video = if (parseVideo) matched.groups["video"] else null
        if (url != null) {
            callback(TextFragment.Url(url.value), s, matched.range.last + 1)
        } else if (video != null) {
            callback(TextFragment.BilibiliVideo(video.value), s, matched.range.last + 1)
        }
        start = matched.range.last + 1
        matched = regex.find(this, start)
    }
    if (start < length) callback(TextFragment.Text(substring(start, length)), start, length)
}

fun SpannableStringBuilder.appendTextAutoLink(text: String) {
    text.mapText(true) { f, _, _ ->
        when (f) {
            is TextFragment.Text -> {
                append(f.text)
            }

            is TextFragment.Url -> {
                val u = f.url.let {
                    if (!it.startsWith("https://") && !it.startsWith("http://"))
                        "http://$it"
                    else it
                }
                append(f.url, MyURLSpan(u), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            is TextFragment.BilibiliVideo -> {
                append(
                    f.id,
                    MyURLSpan("https://www.bilibili.com/video/${f.id}"),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }
}
