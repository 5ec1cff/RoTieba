package io.github.a13e300.ro_tieba.utils

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
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

fun SpannableStringBuilder.appendUser(
    user: User,
    isAuthor: Boolean,
    context: Context,
    showLevel: Boolean = false
) = apply {
    append(user.nick.ifEmpty { user.name })
    if (isAuthor) {
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

