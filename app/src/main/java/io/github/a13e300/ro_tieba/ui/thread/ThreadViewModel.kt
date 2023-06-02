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
import io.github.a13e300.ro_tieba.toPostContent
import io.github.a13e300.ro_tieba.ui.photo.Photo
import tbclient.UserOuterClass
import java.util.Date
import java.util.TreeMap

const val AVATAR_THUMBNAIL = "https://gss0.bdstatic.com/6LZ1dD3d1sgCo2Kml5_Y_D3/sys/portrait/item/"
const val AVATAR_ORIG = "http://tb.himg.baidu.com/sys/portraith/item/"

data class Post(
    val user: User,
    val content: List<Content>,
    val floor: Int,
    val postId: Long,
    val tid: Long,
    val time: Date,
    val comments: List<Comment>,
    val commentCount: Int
) {
    sealed class Content
    data class TextContent(val text: String) : Content()
    data class ImageContent(
        val previewSrc: String,
        val src: String,
        val width: Int,
        val height: Int,
        val order: Int
    ) : Content()

    data class EmojiContent(
        val id: String
    ) : Content()

    data class LinkContent(val text: String, val link: String) : Content()
}

data class Comment(
    val user: User,
    val content: List<Post.Content>,
    val floor: Int,
    val postId: Long,
    val tid: Long,
    val time: Date,
    val ppid: Long
)

data class User(
    val name: String = "unknown",
    val nick: String = "unknown",
    val uid: Long = 0,
    val portrait: String = "",
    val location: String = ""
)

fun UserOuterClass.User.toUser() = this.let { user ->
    User(user.name, user.nameShow, user.id, user.portrait, user.ipAddress)
}

data class ThreadConfig(
    val tid: Long
)

class ThreadViewModel : ViewModel() {
    var currentUid: String? = null
    val threadConfig = MutableLiveData<ThreadConfig>()
    val threadTitle = MutableLiveData<String>()
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
                threadTitle.value = response.thread.title
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
                        if (c is Post.ImageContent) {
                            photos[p.floor to c.order] =
                                Photo(c.src, "t${p.tid}_p${p.postId}_f${p.floor}_c${c.order}")
                        }
                    }
                }
                Logger.d("load thread : $page")
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