package io.github.a13e300.ro_tieba.api

import io.github.a13e300.ro_tieba.api.json.GetFollowForums
import io.github.a13e300.ro_tieba.api.json.SearchForumPost
import io.github.a13e300.ro_tieba.toHexString
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
