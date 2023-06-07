package io.github.a13e300.ro_tieba.api

import io.github.a13e300.ro_tieba.api.web.GetFid
import io.github.a13e300.ro_tieba.api.web.SearchForum
import retrofit2.Call
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Query

interface TiebaWebAPI {
    @FormUrlEncoded
    @GET("/f/commit/share/fnameShareApi?ie=utf-8")
    fun getFid(@Query("fname") forumName: String): Call<GetFid>

    @GET("/mo/q/search/forum")
    suspend fun searchForum(
        @Query("word") keyword: String
    ): SearchForum
}