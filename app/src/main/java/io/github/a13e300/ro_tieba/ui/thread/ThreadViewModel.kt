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
import io.github.a13e300.ro_tieba.api.TiebaClient
import io.github.a13e300.ro_tieba.models.Comment
import io.github.a13e300.ro_tieba.models.Content
import io.github.a13e300.ro_tieba.models.Forum
import io.github.a13e300.ro_tieba.models.Post
import io.github.a13e300.ro_tieba.models.TiebaThread
import io.github.a13e300.ro_tieba.models.User
import io.github.a13e300.ro_tieba.models.toUser
import io.github.a13e300.ro_tieba.toPostContent
import io.github.a13e300.ro_tieba.ui.photo.Photo
import java.util.Date
import java.util.TreeMap

const val AVATAR_THUMBNAIL = "https://gss0.bdstatic.com/6LZ1dD3d1sgCo2Kml5_Y_D3/sys/portrait/item/"
const val AVATAR_ORIG = "http://tb.himg.baidu.com/sys/portraith/item/"

data class ThreadConfig(
    val tid: Long
)

class ThreadViewModel : ViewModel() {
    var currentUid: String? = null
    val threadConfig = MutableLiveData<ThreadConfig>()
    val threadInfo = MutableLiveData<TiebaThread>()
    val photos = TreeMap<Pair<Int, Int>, Photo> { p0, p1 ->
        if (p0.first < p1.first) -1
        else if (p0.first > p1.first) 1
        else if (p0.second < p1.second) -1
        else if (p0.second > p1.second) 1
        else 0
    }

    inner class PostPagingSource(
        private val client: TiebaClient
    ) : PagingSource<Int, Post>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Post> {
            try {
                val page = params.key ?: 1
                val response = client.getPosts(threadConfig.value!!.tid, page)
                threadInfo.value = TiebaThread(
                    tid = response.thread.id,
                    title = response.thread.title,
                    author = response.thread.author.toUser(),
                    content = listOf(),
                    replyNum = response.thread.replyNum,
                    time = Date(response.thread.createTime.toLong() * 1000),
                    postId = response.thread.postId,
                    isGood = response.thread.isGood == 1,
                    forum = Forum(response.forum.name, response.forum.id)
                )
                val users = response.userListList.associateBy({ it.id },
                    { it.toUser() })
                val posts = response.postListList.map { p ->
                    val comments = p.subPostList.subPostListList.map { sp ->
                        Comment(
                            user = users[sp.authorId] ?: User(),
                            content = sp.contentList.toPostContent(),
                            floor = sp.floor,
                            postId = p.id,
                            tid = p.tid,
                            time = Date(sp.time.toLong() * 1000),
                            ppid = p.id
                        )
                    }
                    Post(
                        users[p.authorId] ?: User(),
                        p.contentList.toPostContent(),
                        p.floor,
                        p.id,
                        response.thread.id,
                        Date(p.time.toLong() * 1000),
                        comments,
                        p.subPostNumber
                    )
                }
                posts.forEach { p ->
                    p.content.forEach { c ->
                        if (c is Content.ImageContent) {
                            photos[p.floor to c.order] =
                                Photo(c.src, c.order, p)
                        }
                    }
                }
                return LoadResult.Page(
                    data = posts,
                    prevKey = if (page == 1) null else (page - 1),
                    nextKey = if (response.page.hasMore != 0) page + 1 else null
                )
            } catch (t: Throwable) {
                return LoadResult.Error(t)
            }
        }

        override fun getRefreshKey(state: PagingState<Int, Post>): Int? {
            return state.anchorPosition?.let { anchorPosition ->
                val anchorPage = state.closestPageToPosition(anchorPosition)
                anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
            }
        }
    }

    val flow = Pager(
        PagingConfig(pageSize = 30)
    ) {
        photos.clear()
        PostPagingSource(App.instance.client)
    }.flow
        .cachedIn(viewModelScope)
}