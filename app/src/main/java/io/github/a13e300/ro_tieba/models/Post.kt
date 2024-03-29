package io.github.a13e300.ro_tieba.models

import java.util.Date

data class Post(
    override val user: User,
    override val content: List<Content>,
    override val floor: Int,
    val postId: Long,
    val tid: Long,
    override val time: Date,
    val comments: List<Comment>,
    val commentCount: Int,
    val agreeNum: Long = 0,
    val disagreeNum: Long = 0,
    val page: Int = 0
) : IPost {
    override val id: Long
        get() = postId
}
