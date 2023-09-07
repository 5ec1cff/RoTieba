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

data class UserProfile(
    val name: String = "unknown",
    val nick: String = "unknown",
    val uid: Long = 0,
    val portrait: String = "",
    val desc: String = "",
    val fanNum: Int = 0,
    val followNum: Int = 0,
    val threadNum: Int = 0
) {
    val showName: String
        get() = nick.ifEmpty { name }

    val avatarUrl: String
        get() = "$AVATAR_THUMBNAIL/$portrait"

    val realAvatarUrl: String
        get() = "$AVATAR_ORIG/$portrait"
}

fun UserOuterClass.User.toUserProfile() =
    UserProfile(
        name = name,
        nick = nameShow,
        uid = id,
        portrait = portrait,
        desc = intro,
        fanNum = fansNum,
        followNum = concernNum,
        threadNum = postNum
    )
