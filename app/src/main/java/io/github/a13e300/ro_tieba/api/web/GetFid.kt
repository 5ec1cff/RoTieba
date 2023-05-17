package io.github.a13e300.ro_tieba.api.web

data class GetFid(
    val data: Data
) {
    data class Data(
        val fid: String
    )
}
