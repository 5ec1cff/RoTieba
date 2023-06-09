package io.github.a13e300.ro_tieba.models

import tbclient.UserOuterClass

data class User(
    val name: String = "unknown",
    val nick: String = "unknown",
    val uid: Long = 0,
    val portrait: String = "",
    val location: String = "",
    val level: Int = 0
)

fun UserOuterClass.User.toUser() = this.let { user ->
    User(
        name = user.name,
        nick = user.nameShow,
        uid = user.id,
        portrait = user.portrait,
        location = user.ipAddress,
        level = user.levelId
    )
}
