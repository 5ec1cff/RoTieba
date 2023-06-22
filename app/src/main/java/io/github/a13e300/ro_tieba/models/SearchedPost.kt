package io.github.a13e300.ro_tieba.models

data class SearchedPost(
    val post: Post,
    val title: String,
    val forum: String,
    val content: String
)
