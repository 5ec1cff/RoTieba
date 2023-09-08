package io.github.a13e300.ro_tieba.api.json

import com.google.gson.annotations.SerializedName

data class GetFollowForums(
    @SerializedName("has_more")
    val hasMore: Boolean,
    @SerializedName("forum_list")
    val forumList: ForumList?,
) {
    data class ForumList(
        @SerializedName("non-gconforum")
        val nonGconForum: List<Forum>?,
        @SerializedName("gconforum")
        val gconForum: List<Forum>?
    )

    data class Forum(
        val id: String,
        val name: String,
        @SerializedName("level_id")
        val levelId: String,
        val avatar: String,
        val slogan: String
    )
}
