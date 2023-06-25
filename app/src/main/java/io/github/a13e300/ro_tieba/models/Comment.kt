package io.github.a13e300.ro_tieba.models

import java.util.Date

data class Comment(
    override val user: User,
    override val content: List<Content>,
    override val floor: Int,
    val postId: Long,
    val tid: Long,
    override val time: Date,
    val ppid: Long
) : IPost {
    override val id: Long
        get() = ppid
}