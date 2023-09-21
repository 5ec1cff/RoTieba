package io.github.a13e300.ro_tieba.api.web

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import io.github.a13e300.ro_tieba.api.adapters.NullAdapter

data class SearchUser(
    @JsonAdapter(NullAdapter::class)
    val exactMatch: UserInfo?,
    val fuzzyMatch: List<UserInfo>
) {
    data class UserInfo(
        val id: Long,
        val intro: String,
        @SerializedName("user_nickname")
        val nickname: String,
        val name: String, // empty?
        @SerializedName("show_nickname")
        val showNickName: String,
        @SerializedName("portrait")
        val avatar: String,
        @SerializedName("encry_uid")
        val portrait: String,
        @SerializedName("has_concerned")
        val concerned: Int,
        @SerializedName("fans_num")
        val fansNum: Int
    )
}
