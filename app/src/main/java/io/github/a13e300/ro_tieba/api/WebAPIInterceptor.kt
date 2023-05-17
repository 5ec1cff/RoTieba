package io.github.a13e300.ro_tieba.api

import io.github.a13e300.ro_tieba.api.json.TiebaApiErrorInfo
import io.github.a13e300.ro_tieba.fromJson
import okhttp3.Interceptor
import okhttp3.Response

class WebAPIInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        response.peekBody(Long.MAX_VALUE).string().also {
            val errInfo = it.fromJson(TiebaApiErrorInfo::class.java)
            if (errInfo.errorCode != "0") {
                throw TiebaApiError(errInfo.errorCode.toInt(), errInfo.errorMsg)
            }
        }
        return response
    }
}
