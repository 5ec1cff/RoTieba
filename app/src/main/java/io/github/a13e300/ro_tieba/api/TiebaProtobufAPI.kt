package io.github.a13e300.ro_tieba.api

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface TiebaProtobufAPI {
    @Headers("x_bd_data_type: protobuf")
    @Multipart
    @POST("/c/f/pb/page?cmd=302001")
    suspend fun getPosts(@Part part: MultipartBody.Part): ResponseBody

    @Headers("x_bd_data_type: protobuf")
    @Multipart
    @POST("/c/f/frs/page?cmd=301001")
    suspend fun getThreads(@Part part: MultipartBody.Part): ResponseBody

    @Headers("x_bd_data_type: protobuf")
    @Multipart
    @POST("/c/f/pb/floor?cmd=302002")
    suspend fun getComments(@Part part: MultipartBody.Part): ResponseBody
}