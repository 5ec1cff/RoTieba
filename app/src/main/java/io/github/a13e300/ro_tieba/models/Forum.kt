package io.github.a13e300.ro_tieba.models

data class Forum(
    val name: String,
    val id: Long,
    val avatarUrl: String? = null,
    val desc: String? = null
)