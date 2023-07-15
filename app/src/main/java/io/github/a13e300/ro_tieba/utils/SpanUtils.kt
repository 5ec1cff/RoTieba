package io.github.a13e300.ro_tieba.utils

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.misc.RoundSpan
import io.github.a13e300.ro_tieba.models.User

fun SpannableStringBuilder.appendLevelSpan(
    context: Context,
    level: Int
) = apply {
    append(
        "${level}级", RoundSpan(
            context,
            context.getColor(
                if (level < 4)
                    R.color.level_color_low
                else if (level < 10)
                    R.color.level_color_mid
                else R.color.level_color_high
            ),
            context.getColor(R.color.level_span_text),
            showText = "$level",
            width = context.resources.getDimension(R.dimen.level_span_width)
        ), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
}

fun SpannableStringBuilder.appendUserInfo(
    user: User,
    isAuthor: Boolean,
    context: Context,
    showLevel: Boolean = false
) = apply {
    if (isAuthor) {
        if (user.bawuType?.isNotEmpty() == true) {
            append(" ")
            val bawuTypeName = when (val t = user.bawuType) {
                "manager" -> context.getString(R.string.bawu_type_manager_name)
                "assist" -> context.getString(R.string.bawu_type_assist_name)
                else -> t
            }
            append(
                "[$bawuTypeName]",
                RoundSpan(
                    context,
                    context.getColor(R.color.bawu_span_background),
                    context.getColor(R.color.bawu_span_text),
                    showText = bawuTypeName
                ),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        append(" ")
        append(
            "[楼主]",
            RoundSpan(
                context,
                context.getColor(R.color.user_span_background),
                context.getColor(R.color.user_span_text),
                showText = "楼主"
            ),
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
    if (showLevel && user.level > 0) {
        append(" ")
        appendLevelSpan(context, user.level)
    }
}

fun String.replaceEm(context: Context): CharSequence {
    val sb = SpannableStringBuilder()
    val r = Regex("<em>(.*?)</em>")
    var start = 0
    while (true) {
        val m = r.find(this, start) ?: break
        sb.append(substring(start, m.range.first))
        sb.append(
            m.groupValues[1],
            ForegroundColorSpan(context.getColor(R.color.em_span_color)),
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        start = m.range.last + 1
    }
    sb.append(substring(start))
    return sb
}

