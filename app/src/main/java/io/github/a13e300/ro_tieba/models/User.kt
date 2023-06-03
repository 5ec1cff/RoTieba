package io.github.a13e300.ro_tieba.models

data class User(
    val name: String = "unknown",
    val nick: String = "unknown",
    val uid: Long = 0,
    val portrait: String = "",
    val location: String = ""
)