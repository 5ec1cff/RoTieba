package io.github.a13e300.ro_tieba.api

import io.github.a13e300.ro_tieba.api.web.GetFid
import io.github.a13e300.ro_tieba.api.web.SearchForum
import io.github.a13e300.ro_tieba.api.web.SearchThread
import io.github.a13e300.ro_tieba.api.web.SearchUser
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

    @GET("/mo/q/search/thread")
    suspend fun searchThread(
        @Query("word") keyword: String,
        @Query("pn") page: Int,
        @Query("st") order: String, // 0 -> old, 1 -> new, 2 -> relevant
        @Query("tt") filter: String, // 2 -> all, 1 -> thread
        @Query("ct") ct: String = "2"
    ): SearchThread

    @GET("/mo/q/search/user")
    suspend fun searchUser(
        @Query("word") keyword: String
    ): SearchUser
}
