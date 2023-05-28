package io.github.a13e300.ro_tieba.api

import io.github.a13e300.ro_tieba.api.json.LoginInfo
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface TiebaLoginAPI {
    @FormUrlEncoded
    @POST("/c/s/login")
    suspend fun login(@Field("bdusstoken") bduss: String): LoginInfo
}
