package io.github.a13e300.ro_tieba.models

import java.util.Date

data class Comment(
    val user: User,
    val content: List<Content>,
    val floor: Int,
    val postId: Long,
    val tid: Long,
    val time: Date,
    val ppid: Long
)