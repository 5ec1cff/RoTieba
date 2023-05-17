package io.github.a13e300.ro_tieba.api

import io.github.a13e300.ro_tieba.api.json.GetFollowForums
import io.github.a13e300.ro_tieba.api.json.LoginInfo
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface TiebaJsonAPI {
    @FormUrlEncoded
    @POST("/c/s/login")
    suspend fun login(@Field("bdusstoken") bduss: String): LoginInfo

    @FormUrlEncoded
    @POST("/c/f/forum/like")
    suspend fun getFollowForums(
        @Field("friend_uid") uid: String,
        @Field("page_no") pn: Int,
        @Field("page_size") size: Int
    ): GetFollowForums
}
