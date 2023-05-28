package io.github.a13e300.ro_tieba.api

import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.BuildConfig
import io.github.a13e300.ro_tieba.api.json.TiebaApiErrorInfo
import io.github.a13e300.ro_tieba.db.Account
import io.github.a13e300.ro_tieba.fromJson
import io.github.a13e300.ro_tieba.ignoreAllSSLErrors
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
import tbclient.PbPage.PbPageReqIdlOuterClass.PbPageReqIdl
import tbclient.PbPage.PbPageResIdlOuterClass.PbPageResIdl
import tbclient.PbPage.errorOrNull

class TiebaClient(val account: Account = Account()) {
    companion object {
        const val MAIN_VERSION = "12.40.1.0"
        const val POST_VERSION = "9.1.0.0"

        const val APP_SECURE_SCHEME = "https"
        const val APP_INSECURE_SCHEME = "http"

        const val APP_BASE_HOST = "tiebac.baidu.com"
        const val WEB_BASE_HOST = "tieba.baidu.com"
    }
    private val mClient: OkHttpClient = OkHttpClient.Builder().apply {
        if (BuildConfig.DEBUG)
            this.ignoreAllSSLErrors()
        // TODO: Only Web API require cookies
        /*
        this.cookieJar(object : CookieJar {
            override fun loadForRequest(url: HttpUrl): List<Cookie> =
                account.bduss?.let {
                    listOf(
                        Cookie.Builder()
                            .domain("baidu.com")
                            .path("/")
                            .name("BDUSS")
                            .value(it)
                            .build()
                    )
                } ?: listOf()

            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            }

        })*/
        this.addInterceptor(JsonAPIInterceptor())
    }.build()
    val jsonAPI: TiebaJsonAPI = Retrofit.Builder()
        .baseUrl("$APP_SECURE_SCHEME://$APP_BASE_HOST")
        .addConverterFactory(GsonConverterFactory.create(App.gson))
        .client(mClient)
        .build()
        .create(TiebaJsonAPI::class.java)
    private val webAPI: TiebaWebAPI = Retrofit.Builder()
        .baseUrl("$APP_INSECURE_SCHEME://$WEB_BASE_HOST")
        .addConverterFactory(GsonConverterFactory.create(App.gson))
        .client(mClient)
        .build()
        .create(TiebaWebAPI::class.java)
    private val protobufAPI: TiebaProtobufAPI = Retrofit.Builder()
        .baseUrl("$APP_SECURE_SCHEME://$APP_BASE_HOST")
        .client(mClient)
        .build()
        .create(TiebaProtobufAPI::class.java)

    suspend fun getPosts(tid: Long, page: Int = 1): PbPageResIdl.DataRes {
        val req = PbPageReqIdl.newBuilder()
            .setData(
                PbPageReqIdl.DataReq.newBuilder()
                    .setCommon(
                        CommonReqOuterClass.CommonReq.newBuilder()
                            .setClientType(2)
                            .setClientVersion(MAIN_VERSION)
                    )
                    .setTid(tid)
                    .setPn(page)
                    .setRn(30) // post count
                    .setSort(0)
                    .setOnlyThreadAuthor(0)
                    .setWithComments(0)
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
                    .setFname(fname)
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
