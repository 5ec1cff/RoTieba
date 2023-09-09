package io.github.a13e300.ro_tieba.models

import io.github.a13e300.ro_tieba.DEFAULT_FORUM_AVATAR
import io.github.a13e300.ro_tieba.api.json.GetFollowForums
import io.github.a13e300.ro_tieba.api.web.SearchForum

data class Forum(
    val name: String,
    val id: Long,
    val avatarUrl: String? = null,
    val desc: String? = null
)

fun SearchForum.ForumInfo.toForum(): Forum =
    Forum(forumName, forumId.toLong(), avatar.ifEmpty { DEFAULT_FORUM_AVATAR }, slogan)

data class UserForum(
    val name: String,
    val id: Long,
    val avatarUrl: String? = null,
    val desc: String? = null,
    val levelId: Int
)

fun GetFollowForums.Forum.toUserForum() = UserForum(
    name, id.toLong(), avatar.ifEmpty { DEFAULT_FORUM_AVATAR }, slogan, levelId.toInt()
)
