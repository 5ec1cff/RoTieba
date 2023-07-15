package io.github.a13e300.ro_tieba.api.json

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import io.github.a13e300.ro_tieba.api.adapters.IntBooleanAdapter
import io.github.a13e300.ro_tieba.api.adapters.IntStringAdapter

data class SearchForumPost(
    val page: PageBean,
    @SerializedName("post_list")
    val postList: List<ThreadInfoBean>
) {
    data class ThreadInfoBean(
        val tid: String,
        val pid: String,
        val title: String,
        val content: String,
        val time: String,
        @SerializedName("fname")
        val forumName: String,
        val author: UserInfoBean,
        @SerializedName("thread_type")
        val type: Int,
        @JsonAdapter(IntBooleanAdapter::class)
        @SerializedName("is_floor")
        val isFloor: Boolean,
        @SerializedName("is_replay")
        val isReply: Boolean?,
        val cid: String
    )

    data class UserInfoBean(
        @SerializedName("name")
        val name: String?,
        @SerializedName("name_show")
        val showName: String
    )

    data class PageBean(
        @JsonAdapter(IntStringAdapter::class)
        @SerializedName("page_size")
        val pageSize: Int,
        @JsonAdapter(IntStringAdapter::class)
        val offset: Int,
        @JsonAdapter(IntStringAdapter::class)
        @SerializedName("current_page")
        val currentPage: Int,
        @JsonAdapter(IntStringAdapter::class)
        @SerializedName("total_count")
        val totalCount: Int,
        @JsonAdapter(IntStringAdapter::class)
        @SerializedName("total_page")
        val totalPage: Int,
        @JsonAdapter(IntBooleanAdapter::class)
        @SerializedName("has_more")
        val hasMore: Boolean,
        @JsonAdapter(IntBooleanAdapter::class)
        @SerializedName("has_prev")
        val hasPrev: Boolean
    )
}
