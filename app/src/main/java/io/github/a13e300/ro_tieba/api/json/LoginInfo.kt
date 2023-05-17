package io.github.a13e300.ro_tieba.api.json

data class LoginInfo(
    val user: UserInfo,
    val anti: AntiInfo
) {
    data class UserInfo(
        val id: String,
        val name: String,
        val portrait: String
    )

    data class AntiInfo(
        val tbs: String
    )
}
