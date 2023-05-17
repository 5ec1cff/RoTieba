package io.github.a13e300.ro_tieba.api.json

import com.google.gson.annotations.SerializedName

data class TiebaApiErrorInfo(
    @SerializedName("error_code", alternate = ["no", "errno"])
    val errorCode: String,
    @SerializedName("error_msg", alternate = ["error", "errmsg"])
    val errorMsg: String?
)