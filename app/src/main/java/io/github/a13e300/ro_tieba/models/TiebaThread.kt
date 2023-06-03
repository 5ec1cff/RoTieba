package io.github.a13e300.ro_tieba.models

import java.util.Date

data class TiebaThread(
    val tid: Long,
    val title: String,
    val author: User,
    val content: List<Content>,
    val time: Date,
    val replyNum: Int
)