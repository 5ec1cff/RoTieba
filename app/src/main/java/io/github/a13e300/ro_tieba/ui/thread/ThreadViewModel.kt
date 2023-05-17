package io.github.a13e300.ro_tieba.ui.thread

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

const val AVATAR_THUMBNAIL = "https://gss0.bdstatic.com/6LZ1dD3d1sgCo2Kml5_Y_D3/sys/portrait/item/"
const val AVATAR_ORIG = "http://tb.himg.baidu.com/sys/portraith/item/"

data class Post(
    val user: User,
    val content: List<Content>,
    val floor: Int,
    val postId: Long,
    val tid: Long
) {
    sealed class Content
    data class TextContent(val text: String) : Content()
    data class ImageContent(
        val previewSrc: String,
        val src: String,
        val width: Int,
        val height: Int
    ) : Content()
}

data class User(
    val name: String = "unknown",
    val nick: String = "unknown",
    val uid: Long = 0,
    val portrait: String = ""
)

data class ThreadConfig(
    val tid: Long
)

class ThreadViewModel : ViewModel() {
    val threadConfig = MutableLiveData<ThreadConfig>()
    val threadTitle = MutableLiveData<String>()

    inner class PostPagingSource(
        private val client: TiebaClient
    ) : PagingSource<Int, Post>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Post> {
            val page = params.key ?: 1
            val response = client.getPosts(threadConfig.value!!.tid, page)
            threadTitle.value = response.thread.title
            val users = response.userListList.associateBy({ it.id },
                { User(it.name, it.nameShow, it.id, it.portrait) })
            val posts = response.postListList.map { p ->
                val content = p.contentList.map {
                    if (it.type == 3 || it.type == 20) {
                        val sizes = it.bsize.split(",")
                        Post.ImageContent(
                            it.cdnSrc,
                            it.originSrc,
                            sizes[0].toInt(),
                            sizes[1].toInt()
                        )
                    } else Post.TextContent(it.text)
                }
                Post(users[p.authorId] ?: User(), content, p.floor, p.id, response.thread.id)
            }
            Logger.d("load thread : $page")
            return LoadResult.Page(
                data = posts,
                prevKey = null,
                nextKey = if (response.page.hasMore != 0) page + 1 else null
            )
        }

        override fun getRefreshKey(state: PagingState<Int, Post>): Int? {
            return state.anchorPosition?.let { anchorPosition ->
                val anchorPage = state.closestPageToPosition(anchorPosition)
                anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
            }
        }
    }

    val flow = Pager(
        // Configure how data is loaded by passing additional properties to
        // PagingConfig, such as prefetchDistance.
        PagingConfig(pageSize = 30)
    ) {
        PostPagingSource(App.instance.client)
    }.flow
        .cachedIn(viewModelScope)
}