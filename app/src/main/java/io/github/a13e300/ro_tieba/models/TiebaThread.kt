package io.github.a13e300.ro_tieba.models

import androidx.recyclerview.widget.DiffUtil
import java.util.Date

data class TiebaThread(
    val tid: Long,
    val postId: Long,
    val title: String,
    val author: User,
    val content: List<Content>,
    val time: Date,
    val replyNum: Int,
    val isGood: Boolean,
    val forum: Forum? = null,
    val createTime: Date? = null,
    val viewNum: Int = 0,
    val agreeNum: Long = 0,
    val disagreeNum: Long = 0,
    val images: List<Content.ImageContent> = emptyList(),
    val tabInfo: ForumTab.GeneralTab? = null,
    val threadType: ThreadType = ThreadType.NORMAL,
    val isTop: Boolean = false
)

object ThreadComparator : DiffUtil.ItemCallback<TiebaThread>() {
    override fun areItemsTheSame(oldItem: TiebaThread, newItem: TiebaThread): Boolean {
        return oldItem.tid == newItem.tid
    }

    override fun areContentsTheSame(oldItem: TiebaThread, newItem: TiebaThread): Boolean {
        return oldItem == newItem
    }
}
