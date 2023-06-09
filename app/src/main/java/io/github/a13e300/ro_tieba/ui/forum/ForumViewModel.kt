package io.github.a13e300.ro_tieba.ui.forum

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.api.TiebaClient
import io.github.a13e300.ro_tieba.models.Forum
import io.github.a13e300.ro_tieba.models.TiebaThread
import io.github.a13e300.ro_tieba.models.User
import io.github.a13e300.ro_tieba.toPostContent
import java.util.Date

class ForumViewModel : ViewModel() {
    var currentUid: String? = null
    val forumInfo = MutableLiveData<Forum>()
    lateinit var forumName: String

    inner class ThreadPagingSource(
        private val client: TiebaClient
    ) : PagingSource<Int, TiebaThread>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TiebaThread> {
            val page = params.key ?: 1
            try {
                val response = client.getThreads(forumName, page)
                // TODO: get more bar info
                forumInfo.value = Forum(response.forum.name, response.forum.id, "", "")
                val users = response.userListList.associateBy({ it.id },
                    { User(it.name, it.nameShow, it.id, it.portrait) })
                val posts = response.threadListList.map { p ->
                    TiebaThread(
                        p.id,
                        p.postId,
                        p.title,
                        users[p.authorId] ?: User(),
                        p.firstPostContentList.toPostContent(),
                        Date(p.createTime.toLong() * 1000),
                        p.replyNum
                    )
                }
                return LoadResult.Page(
                    data = posts,
                    prevKey = null,
                    nextKey = if (response.page.hasMore != 0) page + 1 else null
                )
            } catch (t: Throwable) {
                return LoadResult.Error(t)
            }
        }

        override fun getRefreshKey(state: PagingState<Int, TiebaThread>): Int? {
            return state.anchorPosition?.let { anchorPosition ->
                val anchorPage = state.closestPageToPosition(anchorPosition)
                anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
            }
        }
    }

    val flow = Pager(
        PagingConfig(pageSize = 30)
    ) {
        ThreadPagingSource(App.instance.client)
    }.flow
        .cachedIn(viewModelScope)
}