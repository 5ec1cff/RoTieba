package io.github.a13e300.ro_tieba.models

import java.util.Date

data class Reply(
    val content: List<Content>,
    val pid: Long,
    val comment: Boolean,
    val quota: QuotaInfo?,
    val threadId: Long,
    val threadTitle: String,
    val time: Date,
    val forumId: Long,
    val forumName: String
) {
    data class QuotaInfo(
        val pid: Long,
        val uid: Long,
        val username: String,
        val content: String
    )
}
