package io.github.a13e300.ro_tieba.api

import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.BuildConfig
import io.github.a13e300.ro_tieba.db.Account
import io.github.a13e300.ro_tieba.utils.ignoreAllSSLErrors
import okhttp3.FormBody
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TiebaLoginClient {
    private val mClient: OkHttpClient = OkHttpClient.Builder().apply {
        if (BuildConfig.DEBUG)
            this.ignoreAllSSLErrors()
        addInterceptor { chain ->
            val req = chain.request()
            val body = req.body as FormBody
            val newRequest = body.let { form ->
                req.newBuilder()
                    .post(form.toSignatureBody())
                    .build()
            }
            chain.proceed(newRequest)
        }
    }.build()
    private val loginAPI: TiebaLoginAPI = Retrofit.Builder()
        .baseUrl("${TiebaClient.APP_SECURE_SCHEME}://${TiebaClient.APP_BASE_HOST}")
        .addConverterFactory(GsonConverterFactory.create(App.gson))
        .client(mClient)
        .build()
        .create(TiebaLoginAPI::class.java)

    suspend fun login(bduss: String, sToken: String? = null, baiduId: String? = null): Account {
        val r = loginAPI.login(bduss)
        return Account(
            uid = r.user.id,
            name = r.user.name,
            portrait = r.user.portrait,
            tbs = r.anti.tbs,
            bduss = bduss,
            stoken = sToken,
            baiduId = baiduId
        )
    }
}
