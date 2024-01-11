package io.github.a13e300.ro_tieba.models

import java.util.Date

data class SearchedPost(
    val user: User,
    val id: PostId,
    val time: Date,
    val title: String,
    val forum: Forum,
    val content: List<Content>
)
