package io.github.a13e300.ro_tieba.api.json

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import io.github.a13e300.ro_tieba.api.adapters.IntBooleanAdapter
import io.github.a13e300.ro_tieba.api.adapters.IntStringAdapter

data class UserPostResponse(
    @JsonAdapter(IntBooleanAdapter::class)
    @SerializedName("hide_post")
    val hidePost: Boolean,
    @SerializedName("post_list")
    val postList: List<Post> // post for each forum
) {
    data class Post(
        val content: List<Content>,
        @SerializedName("forum_id")
        @JsonAdapter(IntStringAdapter::class)
        val forumId: Long,
        @SerializedName("forum_name")
        val forumName: String,
        @JsonAdapter(IntStringAdapter::class)
        @SerializedName("freq_num")
        val freqNum: Int,
        @JsonAdapter(IntBooleanAdapter::class)
        @SerializedName("is_thread")
        val isThread: Boolean,
        @JsonAdapter(IntStringAdapter::class)
        @SerializedName("thread_id")
        val threadId: Long,
        @JsonAdapter(IntStringAdapter::class)
        @SerializedName("thread_type")
        val threadType: Int,
        val title: String,
        @JsonAdapter(IntStringAdapter::class)
        @SerializedName("reply_num")
        val replyNum: Int,
        @JsonAdapter(IntStringAdapter::class)
        @SerializedName("create_time")
        val createTime: Long,
        val quota: Quota?
        // user_id, user_name, user_portrait are useless
    )

    data class Content(
        @JsonAdapter(IntStringAdapter::class)
        @SerializedName("post_id")
        val postId: Long,
        @JsonAdapter(IntStringAdapter::class)
        @SerializedName("post_type")
        val postType: Int, // 0 -> floor, 1 -> floor in floor (has quota)
        @SerializedName("post_content")
        val postContent: List<PostContent>
    )

    data class PostContent(
        val type: String,
        val text: String,
        @JsonAdapter(IntStringAdapter::class)
        val uid: Long?,
        val bsize: String?, // w,h
        val src: String?
    )

    data class Quota(
        val content: String,
        @JsonAdapter(IntStringAdapter::class)
        @SerializedName("post_id")
        val postId: Long,
        @JsonAdapter(IntStringAdapter::class)
        @SerializedName("user_id")
        val userId: Long,
        @SerializedName("user_name")
        val userName: String
    )

    data class PrivSets(
        @JsonAdapter(IntStringAdapter::class)
        val like: Int?,
        @JsonAdapter(IntStringAdapter::class)
        val location: Int?,
        @JsonAdapter(IntStringAdapter::class)
        val post: Int?
    )
}
