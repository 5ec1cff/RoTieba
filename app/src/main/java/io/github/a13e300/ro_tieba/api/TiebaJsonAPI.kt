package io.github.a13e300.ro_tieba.api

import io.github.a13e300.ro_tieba.api.json.GetFollowForums
import io.github.a13e300.ro_tieba.api.json.SearchForumPost
import io.github.a13e300.ro_tieba.api.json.UserPostResponse
import io.github.a13e300.ro_tieba.utils.toHexString
import okhttp3.FormBody
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import java.security.MessageDigest
import java.util.TreeMap

interface TiebaJsonAPI {
    @FormUrlEncoded
    @POST("/c/f/forum/like")
    suspend fun getFollowForums(
        @Field("friend_uid") uid: String,
        @Field("page_no") pn: Int,
        @Field("page_size") size: Int
    ): GetFollowForums

    @FormUrlEncoded
    @POST("/c/s/searchpost")
    suspend fun searchForumPost(
        @Field("kw") kw: String,
        @Field("word") word: String,
        @Field("onlyThread") onlyThread: String,
        @Field("pn") pn: Int,
        @Field("rn") rn: Int,
        @Field("sm") order: String
    ): SearchForumPost

    @FormUrlEncoded
    @POST("/c/u/feed/userpost")
    suspend fun userPost(
        @Field("uid") uid: String,
        @Field("pn") page: Int = 1,
        @Field("is_thread") isThread: Int,
        @Field("rn") pageSize: Int = 20,
        @Field("need_content") need_content: Int = 1,
        // We must use lower version, so that we can get posts
        @Field("_client_version") _client_version: String = "7.2.0.0",
        @Field("_client_type") _client_type: String = "2",
        @Field("subapp_type") _subapp_type: String = "mini"
    ): UserPostResponse
}

fun FormBody.toSignatureBody(bduss: String? = null): FormBody {
    val form = this
    val realKeyValue = TreeMap<String, String>()
    realKeyValue["_client_version"] = TiebaClient.MAIN_VERSION
    val md5 = MessageDigest.getInstance("md5")
    for (i in 0 until form.size) {
        val key = form.name(i)
        val value = form.value(i)
        realKeyValue[key] = value
    }
    bduss?.let { realKeyValue["BDUSS"] = it }
    val newForm = FormBody.Builder()
    realKeyValue.forEach { (k, v) ->
        md5.update(k.toByteArray())
        md5.update("=".toByteArray())
        md5.update(v.toByteArray())
        newForm.add(k, v)
    }
    md5.update("tiebaclient!!!".toByteArray())
    newForm.add("sign", md5.digest().toHexString())
    return newForm.build()
}
