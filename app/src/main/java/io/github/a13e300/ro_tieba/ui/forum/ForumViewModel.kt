package io.github.a13e300.ro_tieba.ui.forum

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
import io.github.a13e300.ro_tieba.ui.thread.User

data class TiebaThread(
    val tid: Long,
    val title: String,
    val author: User,
    val content: String
)

class ForumViewModel : ViewModel() {
    lateinit var forumName: String

    inner class ThreadPagingSource(
        private val client: TiebaClient
    ) : PagingSource<Int, TiebaThread>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TiebaThread> {
            val page = params.key ?: 1
            Logger.d("load thread : $forumName $page")
            val response = client.getThreads(forumName, page)
            val users = response.userListList.associateBy({ it.id },
                { User(it.name, it.nameShow, it.id, it.portrait) })
            val posts = response.threadListList.map { p ->
                val content = p.firstPostContentList.joinToString("") { it.text }
                TiebaThread(p.id, p.title, users[p.authorId] ?: User(), content)
            }
            return LoadResult.Page(
                data = posts,
                prevKey = null,
                nextKey = if (response.page.hasMore != 0) page + 1 else null
            )
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