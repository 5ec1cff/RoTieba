package io.github.a13e300.ro_tieba.api

import android.annotation.SuppressLint
import android.util.Log
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.BuildConfig
import io.github.a13e300.ro_tieba.api.json.TiebaApiErrorInfo
import io.github.a13e300.ro_tieba.db.Account
import io.github.a13e300.ro_tieba.fromJson
import io.github.a13e300.ro_tieba.toHexString
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
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
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.TreeMap
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


const val TAG = "TiebaApi"

@SuppressLint("CustomX509TrustManager")
fun OkHttpClient.Builder.ignoreAllSSLErrors(): OkHttpClient.Builder {
    val naiveTrustManager = object : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) = Unit
        override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) = Unit
    }

    val insecureSocketFactory = SSLContext.getInstance("TLSv1.2").apply {
        val trustAllCerts = arrayOf<TrustManager>(naiveTrustManager)
        init(null, trustAllCerts, SecureRandom())
    }.socketFactory

    sslSocketFactory(insecureSocketFactory, naiveTrustManager)
    hostnameVerifier { _, _ -> true }
    return this
}

class TiebaClient(account: Account = Account()) {
    companion object {
        const val MAIN_VERSION = "12.40.1.0"
        const val POST_VERSION = "9.1.0.0"

        const val APP_SECURE_SCHEME = "https"
        const val APP_INSECURE_SCHEME = "http"

        const val APP_BASE_HOST = "tiebac.baidu.com"
        const val WEB_BASE_HOST = "tieba.baidu.com"
    }

    private var mAccount = account
    fun getAccount() = mAccount
    private val mClient: OkHttpClient = OkHttpClient.Builder().apply {
        if (BuildConfig.DEBUG)
            this.ignoreAllSSLErrors()
        // TODO: Only Web API require cookies
        this.cookieJar(object : CookieJar {
            override fun loadForRequest(url: HttpUrl): List<Cookie> =
                mAccount.bduss?.let {
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

        })
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


    fun getPosts() = runCatching {
        val body = PbPageReqIdl.newBuilder()
            .setData(
                PbPageReqIdl.DataReq.newBuilder()
                    .setCommon(
                        CommonReqOuterClass.CommonReq.newBuilder()
                            .setClientType(2)
                            .setClientVersion(MAIN_VERSION)
                    )
                    .setTid(8223016861)
                    .setPn(1)
                    .setRn(30) // post count
                    .setSort(0)
                    .setOnlyThreadAuthor(1)
                    .setWithComments(0)
                    .setIsFold(0)
            ).build()
        val multipart = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("data", "file", body.toByteArray().toRequestBody())
            .build()
        val req = Request.Builder()
            .url("$APP_SECURE_SCHEME://$APP_BASE_HOST/c/f/pb/page?cmd=302001")
            .header("x_bd_data_type", "protobuf")
            .post(multipart)
            .build()
        val resp = mClient.newCall(req).execute().let {
            PbPageResIdl.parseFrom(it.body!!.byteStream())
        }
        if (resp.errorOrNull?.errorno != 0) {
            Log.e(TAG, "error occurred: ${resp.error.errorno} ${resp.error.errmsg}")
        } else {
            Log.i(TAG, "thread title: ${resp.data.thread.title}")
        }
    }.onFailure {
        Log.e(TAG, "failed to request:", it)
    }

    suspend fun login(bduss: String): Account {
        val r = jsonAPI.login(bduss)
        mAccount = Account(
            uid = r.user.id,
            name = r.user.name,
            portrait = r.user.portrait,
            tbs = r.anti.tbs,
            bduss = bduss
        )
        return mAccount
    }

    inner class JsonAPIInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val req = chain.request()
            val body = req.body
            if (body !is FormBody)
                return chain.proceed(req)
            val newRequest = body.let { form ->
                val realKeyValue = TreeMap<String, String>()
                realKeyValue["_client_version"] = MAIN_VERSION
                val md5 = MessageDigest.getInstance("md5")
                for (i in 0 until form.size) {
                    val key = form.name(i)
                    val value = form.value(i)
                    realKeyValue[key] = value
                }
                if ("bdusstoken" !in realKeyValue)
                    mAccount.bduss?.let { realKeyValue["BDUSS"] = it }
                val newForm = FormBody.Builder()
                realKeyValue.forEach { (k, v) ->
                    md5.update(k.toByteArray())
                    md5.update("=".toByteArray())
                    md5.update(v.toByteArray())
                    newForm.add(k, v)
                }
                md5.update("tiebaclient!!!".toByteArray())
                newForm.add("sign", md5.digest().toHexString())
                req.newBuilder()
                    .post(newForm.build())
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
