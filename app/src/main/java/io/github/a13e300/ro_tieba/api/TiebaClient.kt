package io.github.a13e300.ro_tieba.api

import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.api.json.TiebaApiErrorInfo
import io.github.a13e300.ro_tieba.db.Account
import io.github.a13e300.ro_tieba.fromJson
import io.github.a13e300.ro_tieba.ignoreAllSSLErrorsIfDebug
import kotlinx.coroutines.runBlocking
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import tbclient.CommonReqOuterClass
import tbclient.FrsPage.FrsPageReqIdlOuterClass.FrsPageReqIdl
import tbclient.FrsPage.FrsPageResIdlOuterClass.FrsPageResIdl
import tbclient.FrsPage.errorOrNull
import tbclient.PbFloor.PbFloorReqIdlOuterClass.PbFloorReqIdl
import tbclient.PbFloor.PbFloorResIdlOuterClass.PbFloorResIdl
import tbclient.PbFloor.errorOrNull
import tbclient.PbPage.PbPageReqIdlOuterClass.PbPageReqIdl
import tbclient.PbPage.PbPageResIdlOuterClass.PbPageResIdl
import tbclient.PbPage.errorOrNull
import tbclient.Profile.ProfileReqIdlOuterClass.ProfileReqIdl
import tbclient.Profile.ProfileResIdlOuterClass.ProfileResIdl
import tbclient.Profile.errorOrNull
import java.net.URLEncoder

class TiebaClient(val account: Account = Account()) {
    companion object {
        const val MAIN_VERSION = "12.43.7.0"
        const val POST_VERSION = "9.1.0.0"

        const val APP_SECURE_SCHEME = "https"
        const val APP_INSECURE_SCHEME = "http"

        const val APP_BASE_HOST = "tiebac.baidu.com"
        const val WEB_BASE_HOST = "tieba.baidu.com"
    }

    val jsonAPI: TiebaJsonAPI = Retrofit.Builder()
        .baseUrl("$APP_SECURE_SCHEME://$APP_BASE_HOST")
        .addConverterFactory(GsonConverterFactory.create(App.gson))
        .client(
            OkHttpClient.Builder().ignoreAllSSLErrorsIfDebug().addInterceptor(JsonAPIInterceptor())
                .build()
        )
        .build()
        .create(TiebaJsonAPI::class.java)
    val webAPI: TiebaWebAPI = Retrofit.Builder()
        .baseUrl("$APP_SECURE_SCHEME://$WEB_BASE_HOST")
        .addConverterFactory(WebAPIResultConverterFactory(App.gson))
        .client(
            OkHttpClient.Builder().ignoreAllSSLErrorsIfDebug().addInterceptor(JsonAPIInterceptor())
                .build()
        )
        .build()
        .create(TiebaWebAPI::class.java)
    private val protobufAPI: TiebaProtobufAPI = Retrofit.Builder()
        .baseUrl("$APP_SECURE_SCHEME://$APP_BASE_HOST")
        .client(OkHttpClient.Builder().ignoreAllSSLErrorsIfDebug().build())
        .build()
        .create(TiebaProtobufAPI::class.java)

    suspend fun getPosts(
        tid: Long,
        page: Int = 1,
        pid: Long = 0,
        rn: Int = 30,
        sort: Int = 0, // 0 -> normal, 1 -> reverse, 2 -> hot
        seeLz: Boolean = false
    ): PbPageResIdl.DataRes {
        val req = PbPageReqIdl.newBuilder()
            .setData(
                PbPageReqIdl.DataReq.newBuilder()
                    .setCommon(
                        CommonReqOuterClass.CommonReq.newBuilder()
                            .setClientType(2)
                            .setClientVersion(MAIN_VERSION)
                    )
                    .setTid(tid)
                    .apply {
                        if (pid != 0L)
                            setPid(pid)
                        if (page != 0)
                            pn = page
                    }
                    .setRn(rn) // post count
                    .setSort(sort)
                    .setOnlyThreadAuthor(if (seeLz) 1 else 0)
                    .setWithComments(1)
                    .setCommentRn(4) // sub floors
                    .setIsFold(0)
            ).build()
        val part =
            MultipartBody.Part.createFormData("data", "file", req.toByteArray().toRequestBody())
        val result = protobufAPI.getPosts(part).let {
            PbPageResIdl.parseFrom(it.byteStream())
        }
        if (result.errorOrNull?.errorno != 0) throw TiebaApiError(
            result.error.errorno,
            result.error.errmsg
        )
        return result.data
    }

    suspend fun getThreads(fname: String, pn: Int): FrsPageResIdl.DataRes {
        val req = FrsPageReqIdl.newBuilder()
            .setData(
                FrsPageReqIdl.DataReq.newBuilder()
                    .setCommon(
                        CommonReqOuterClass.CommonReq.newBuilder()
                            .setClientType(2)
                            .setClientVersion(MAIN_VERSION)
                    )
                    .setFname(URLEncoder.encode(fname, "UTF-8"))
                    .setPn(pn)
                    .setRn(30)
                    .setRnNeed(30)
                    .setIsGood(0)
                    .setSort(5) // https://github.com/Starry-OvO/aiotieba/blob/ed8867f6ac73b523389dd1dcbdd4b5f62a16ff81/aiotieba/client.py
            ).build()
        val part =
            MultipartBody.Part.createFormData("data", "file", req.toByteArray().toRequestBody())
        val result = protobufAPI.getThreads(part).let {
            FrsPageResIdl.parseFrom(it.byteStream())
        }
        if (result.errorOrNull?.errorno != 0) throw TiebaApiError(
            result.error.errorno,
            result.error.errmsg
        )
        return result.data
    }

    suspend fun getComments(tid: Long, pid: Long, pn: Int): PbFloorResIdl.DataRes {
        val req = PbFloorReqIdl.newBuilder()
            .setData(
                PbFloorReqIdl.DataReq.newBuilder()
                    .setCommon(
                        CommonReqOuterClass.CommonReq.newBuilder()
                            .setClientType(2)
                            .setClientVersion(MAIN_VERSION)
                    )
                    .setTid(tid)
                    .setPid(pid)
                    .setPn(pn)
            ).build()
        val part =
            MultipartBody.Part.createFormData("data", "file", req.toByteArray().toRequestBody())
        val result = protobufAPI.getComments(part).let {
            PbFloorResIdl.parseFrom(it.byteStream())
        }
        if (result.errorOrNull?.errorno != 0) throw TiebaApiError(
            result.error.errorno,
            result.error.errmsg
        )
        return result.data
    }

    suspend fun getUserProfile(
        portrait: String? = null,
        uid: Long = 0L,
        pn: Int = 1,
        page: Int = 1
    ): ProfileResIdl.DataRes {
        val req = ProfileReqIdl.newBuilder()
            .setData(
                ProfileReqIdl.DataReq.newBuilder()
                    .setCommon(
                        CommonReqOuterClass.CommonReq.newBuilder()
                            .setClientType(2)
                            .setClientVersion(MAIN_VERSION)
                    )
                    .setNeedPostCount(1)
                    .setPn(pn)
                    .setPage(page)
                    .apply {
                        if (portrait != null) setFriendUidPortrait(portrait)
                        if (uid != 0L) setUid(uid)
                    }
            ).build()
        val part =
            MultipartBody.Part.createFormData("data", "file", req.toByteArray().toRequestBody())
        val result = protobufAPI.getUserProfile(part).let {
            ProfileResIdl.parseFrom(it.byteStream())
        }
        if (result.errorOrNull?.errorno != 0) throw TiebaApiError(
            result.error.errorno,
            result.error.errmsg
        )
        return result.data
    }

    // for debug

    fun getThreadsSync(fname: String, pn: Int) = runBlocking {
        getThreads(fname, pn)
    }

    fun getPostsSync(tid: Long, page: Int, pid: Long, rn: Int, sort: Int, seeLz: Boolean) =
        runBlocking {
            getPosts(tid, page, pid, rn, sort, seeLz)
    }

    fun getUserProfileSync(portrait: String?, uid: Long, pn: Int = 1, page: Int) = runBlocking {
        getUserProfile(portrait, uid, pn, page)
    }

    inner class JsonAPIInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val req = chain.request()
            val body = req.body
            if (body !is FormBody)
                return chain.proceed(req)
            val newRequest = body.let { form ->
                req.newBuilder()
                    .post(form.toSignatureBody(account.bduss))
                    .build()
            }
            val response = chain.proceed(newRequest)
            response.peekBody(Long.MAX_VALUE).string().also {
                val errInfo = it.fromJson(TiebaApiErrorInfo::class.java)
                if (errInfo.errorCode != "0") {
                    throw TiebaApiError(errInfo.errorCode.toInt(), errInfo.errorMsg)
                }
            }
            return response
        }
    }
}
