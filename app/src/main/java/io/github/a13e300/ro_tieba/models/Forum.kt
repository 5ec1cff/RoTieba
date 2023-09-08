package io.github.a13e300.ro_tieba.models

import io.github.a13e300.ro_tieba.api.json.GetFollowForums

data class Forum(
    val name: String,
    val id: Long,
    val avatarUrl: String? = null,
    val desc: String? = null
)

data class UserForum(
    val name: String,
    val id: Long,
    val avatarUrl: String? = null,
    val desc: String? = null,
    val levelId: Int
)

fun GetFollowForums.Forum.toUserForum() = UserForum(
    name, id.toLong(), avatar, slogan, levelId.toInt()
)
