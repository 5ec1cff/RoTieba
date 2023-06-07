package io.github.a13e300.ro_tieba.api.web

import com.google.gson.annotations.SerializedName

data class WebApiResult<T>(
    @SerializedName("no")
    val errorCode: Int,
    @SerializedName("error")
    val errorMsg: String,
    val data: T
)
