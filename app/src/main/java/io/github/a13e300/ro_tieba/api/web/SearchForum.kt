package io.github.a13e300.ro_tieba.api.web

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import io.github.a13e300.ro_tieba.api.adapters.NullAdapter

data class SearchForum(
    @JsonAdapter(NullAdapter::class)
    val exactMatch: ForumInfo?,
    val fuzzyMatch: List<ForumInfo>
) {
    data class ForumInfo(
        @SerializedName("forum_id")
        val forumId: Int,
        @SerializedName("forum_name")
        val forumName: String,
        val avatar: String,
        val intro: String?,
        val slogan: String?
    )
}
