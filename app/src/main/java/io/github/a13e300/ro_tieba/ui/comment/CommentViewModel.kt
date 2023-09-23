package io.github.a13e300.ro_tieba.ui.comment

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.Logger
import io.github.a13e300.ro_tieba.api.TiebaClient
import io.github.a13e300.ro_tieba.models.Comment
import io.github.a13e300.ro_tieba.models.Post
import io.github.a13e300.ro_tieba.models.toUser
import io.github.a13e300.ro_tieba.utils.toPostContent
import java.util.Date

sealed class CommentItem {
    data class Comment(val comment: io.github.a13e300.ro_tieba.models.Comment) : CommentItem()
    data class Post(val post: io.github.a13e300.ro_tieba.models.Post) : CommentItem()
}

class CommentViewModel : ViewModel() {
    var pid = 0L
    var tid = 0L
    val commentCount = MutableLiveData<Int>()
    val floor = MutableLiveData<Int>()
    private var post: Post? = null
    var threadAuthorUid: Long = 0L

    inner class CommentPagingSource(
        private val client: TiebaClient
    ) : PagingSource<Int, CommentItem>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CommentItem> {
            val page = params.key ?: 1
            if (page == 0) {
                return LoadResult.Page(
                    data = post?.let { listOf(CommentItem.Post(it)) } ?: emptyList(),
                    prevKey = null,
                    nextKey = page + 1
                )
            }
            try {
                val response = client.getComments(tid, pid, page)
                val p = response.post
                if (post == null) post = Post(
                    p.author.toUser(),
                    p.contentList.toPostContent(),
                    p.floor,
                    p.id,
                    response.thread.id,
                    Date(p.time.toLong() * 1000),
                    emptyList(),
                    p.subPostNumber,
                    agreeNum = p.agree.agreeNum,
                    disagreeNum = p.agree.disagreeNum,
                    page = 0
                )
                if (threadAuthorUid == 0L) {
                    threadAuthorUid = response.thread.author.id
                }
                floor.postValue(response.post.floor)
                commentCount.postValue(response.page.totalCount)
                val comments = response.subpostListList.map { sp ->
                    CommentItem.Comment(
                        Comment(
                        user = sp.author.toUser(),
                        content = sp.contentList.toPostContent(),
                        floor = sp.floor,
                        postId = pid,
                        tid = tid,
                        time = Date(sp.time.toLong() * 1000),
                        ppid = sp.id
                        )
                    )
                }
                return LoadResult.Page(
                    data = comments,
                    prevKey = page - 1,
                    nextKey = if (response.page.currentPage < response.page.totalPage) page + 1 else null
                )
            } catch (t: Throwable) {
                Logger.e("failed to load comment of $pid $tid $page", t)
                return LoadResult.Error(t)
            }
        }

        override fun getRefreshKey(state: PagingState<Int, CommentItem>): Int? {
            return null
        }
    }

    val flow = Pager(
        PagingConfig(pageSize = 30)
    ) {
        post = null
        CommentPagingSource(App.instance.client)
    }.flow
        .cachedIn(viewModelScope)
}