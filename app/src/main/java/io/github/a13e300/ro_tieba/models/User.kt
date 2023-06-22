package io.github.a13e300.ro_tieba.models

import tbclient.UserOuterClass

const val AVATAR_THUMBNAIL = "https://gss0.bdstatic.com/6LZ1dD3d1sgCo2Kml5_Y_D3/sys/portrait/item/"
const val AVATAR_ORIG = "http://tb.himg.baidu.com/sys/portraith/item/"

data class User(
    val name: String = "unknown",
    val nick: String = "unknown",
    val uid: Long = 0,
    val portrait: String = "",
    val location: String = "",
    val level: Int = 0,
    val bawuType: String? = null,
    val avatar: String? = null
) {
    val showName: String
        get() = nick.ifEmpty { name }

    val avatarUrl: String
        get() = avatar ?: "$AVATAR_THUMBNAIL/$portrait"
}

fun UserOuterClass.User.toUser() = this.let { user ->
    User(
        name = user.name,
        nick = user.nameShow,
        uid = user.id,
        portrait = user.portrait,
        location = user.ipAddress,
        level = user.levelId,
        bawuType = user.bawuType
    )
}
