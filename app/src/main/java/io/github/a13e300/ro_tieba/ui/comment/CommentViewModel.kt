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
import io.github.a13e300.ro_tieba.toPostContent
import io.github.a13e300.ro_tieba.ui.thread.Comment
import io.github.a13e300.ro_tieba.ui.thread.toUser
import java.util.Date

class CommentViewModel : ViewModel() {
    var pid = 0L
    var tid = 0L
    val commentCount = MutableLiveData<Int>()

    inner class CommentPagingSource(
        private val client: TiebaClient
    ) : PagingSource<Int, Comment>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Comment> {
            val page = params.key ?: 1
            try {
                val response = client.getComments(tid, pid, page)
                commentCount.postValue(response.page.totalCount)
                val comments = response.subpostListList.map { sp ->
                    Comment(
                        user = sp.author.toUser(),
                        content = sp.contentList.toPostContent(),
                        floor = sp.floor,
                        postId = pid,
                        tid = tid,
                        time = Date(sp.time.toLong() * 1000),
                        ppid = sp.id
                    )
                }
                Logger.d("load comment of $pid $tid : $page")
                return LoadResult.Page(
                    data = comments,
                    prevKey = null,
                    nextKey = if (response.page.currentPage < response.page.totalPage) page + 1 else null
                )
            } catch (t: Throwable) {
                Logger.e("failed to load comment of $pid $tid $page", t)
                return LoadResult.Error(t)
            }
        }

        override fun getRefreshKey(state: PagingState<Int, Comment>): Int? {
            return state.anchorPosition?.let { anchorPosition ->
                val anchorPage = state.closestPageToPosition(anchorPosition)
                anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
            }
        }
    }

    val flow = Pager(
        PagingConfig(pageSize = 30)
    ) {
        CommentPagingSource(App.instance.client)
    }.flow
        .cachedIn(viewModelScope)
}