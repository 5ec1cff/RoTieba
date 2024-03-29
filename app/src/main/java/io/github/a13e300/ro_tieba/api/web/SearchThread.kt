package io.github.a13e300.ro_tieba.api.web

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import io.github.a13e300.ro_tieba.api.adapters.IntBooleanAdapter
import io.github.a13e300.ro_tieba.api.adapters.IntStringAdapter

data class SearchThread(
    @JsonAdapter(IntBooleanAdapter::class)
    @SerializedName("has_more")
    val hasMore: Boolean,
    @SerializedName("current_page")
    val currentPage: Int,
    @SerializedName("post_list")
    val postList: List<ThreadInfoBean>
) {
    data class ThreadInfoBean(
        val tid: String,
        val pid: String,
        val title: String,
        val content: String,
        val time: String,
        @SerializedName("post_num")
        val postNum: String,
        @SerializedName("like_num")
        val likeNum: String,
        @SerializedName("share_num")
        val shareNum: String,
        @SerializedName("forum_id")
        @JsonAdapter(IntStringAdapter::class)
        val forumId: Long,
        @SerializedName("forum_name")
        val forumName: String,
        val user: UserInfoBean,
        val type: Int,
        @SerializedName("forum_info")
        val forumInfo: ForumInfo,
        val cid: String
        // main_post : { title, content }
        // media : [ { big_pic, height, size, small_pic, type, water_pic, width } ]
    )

    data class ForumInfo(
        @SerializedName("forum_name")
        val forumName: String,
        val avatar: String,
    )

    data class UserInfoBean(
        @SerializedName("user_name")
        val userName: String?,
        @SerializedName("show_nickname")
        val showNickname: String,
        @SerializedName("user_id")
        val userId: String?,
        val portrait: String?,
    )
}

enum class SearchFilter(val value: String) {
    ALL("2"), THREAD("1")
}

enum class SearchOrder(val value: String) {
    OLD("0"), NEW("1"), RELEVANT("2")
}
