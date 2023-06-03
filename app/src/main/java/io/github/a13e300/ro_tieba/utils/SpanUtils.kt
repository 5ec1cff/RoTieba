package io.github.a13e300.ro_tieba.utils

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.misc.RoundSpan
import io.github.a13e300.ro_tieba.models.User

fun SpannableStringBuilder.appendUser(user: User, isAuthor: Boolean, context: Context) = apply {
    append(user.nick.ifEmpty { user.name })
    if (isAuthor) {
        append(
            "楼主",
            RoundSpan(
                context.getColor(R.color.user_span_background),
                context.getColor(R.color.user_span_text)
            ),
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
}

