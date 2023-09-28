package io.github.a13e300.ro_tieba.ui.thread

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.insertHeaderItem
import androidx.paging.map
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.Logger
import io.github.a13e300.ro_tieba.api.TiebaClient
import io.github.a13e300.ro_tieba.models.Comment
import io.github.a13e300.ro_tieba.models.Content
import io.github.a13e300.ro_tieba.models.Forum
import io.github.a13e300.ro_tieba.models.Post
import io.github.a13e300.ro_tieba.models.TiebaThread
import io.github.a13e300.ro_tieba.models.User
import io.github.a13e300.ro_tieba.models.toUser
import io.github.a13e300.ro_tieba.ui.photo.Photo
import io.github.a13e300.ro_tieba.utils.toPostContent
import kotlinx.coroutines.flow.map
import java.util.Date
import java.util.TreeMap


data class ThreadConfig(
    val tid: Long,
    val pid: Long = 0L,
    val reverse: Boolean = false,
    val page: Int = 0,
    val seeLz: Boolean = false
)

class ThreadViewModel : ViewModel() {
    var requestedScrollToPid: Long = 0L
    var totalPage = 0
    var currentUid: String? = null
    lateinit var threadConfig: ThreadConfig
    val threadInfo = MutableLiveData<TiebaThread>()
    val photos = TreeMap<Pair<Int, Int>, Photo> { p0, p1 ->
        if (p0.first < p1.first) -1
        else if (p0.first > p1.first) 1
        else if (p0.second < p1.second) -1
        else if (p0.second > p1.second) 1
        else 0
    }

    sealed class PostModel {
        data class Post(val post: io.github.a13e300.ro_tieba.models.Post) : PostModel()
        data object Header : PostModel()
    }

    inner class PostPagingSource(
        private val client: TiebaClient,
        private val threadConfig: ThreadConfig
    ) : PagingSource<Int, Post>() {
        private suspend fun getInitialPage(): Int {
            val pid = threadConfig.pid
            val pageOfPid = if (pid != 0L) {
                // request the page of pid
                try {
                    val pageInfo = client.getPosts(
                        threadConfig.tid,
                        0, pid, sort = if (threadConfig.reverse) 1 else 0,
                        seeLz = threadConfig.seeLz
                    ).page
                    totalPage = pageInfo.totalPage
                    requestedScrollToPid = pid
                    pageInfo.currentPage
                } catch (t: Throwable) {
                    // maybe pid not exists
                    Logger.e("failed to find page of pid $pid in thread ${threadConfig.tid}", t)
                    0
                }
            } else threadConfig.page
            return if (pageOfPid != 0) {
                pageOfPid
            } else if (threadConfig.reverse) {
                // request total page
                val pageInfo = client.getPosts(
                    threadConfig.tid,
                    0, pid, sort = 1, seeLz = threadConfig.seeLz
                ).page
                totalPage = pageInfo.totalPage
                pageInfo.totalPage
            } else 1
        }

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Post> {
            val key = params.key ?: getInitialPage()
            return try {
                val response = client.getPosts(
                    threadConfig.tid, key,
                    sort = if (threadConfig.reverse) 1 else 0, seeLz = threadConfig.seeLz
                )
                threadInfo.value = TiebaThread(
                    tid = response.thread.id,
                    title = response.thread.title,
                    author = response.thread.author.toUser(),
                    content = listOf(),
                    replyNum = response.thread.replyNum,
                    time = Date(response.thread.createTime.toLong() * 1000), // TODO: remove this useless date
                    postId = response.thread.postId,
                    isGood = response.thread.isGood == 1,
                    forum = Forum(response.forum.name, response.forum.id),
                    createTime = Date(response.thread.createTime.toLong() * 1000),
                    agreeNum = response.thread.agree.agreeNum,
                    disagreeNum = response.thread.agree.disagreeNum
                )
                totalPage = response.page.totalPage
                val currentPage = response.page.currentPage
                val users = response.userListList.associateBy({ it.id },
                    { it.toUser() })
                val posts = response.postListList.map { p ->
                    val comments = p.subPostList.subPostListList.map { sp ->
                        Comment(
                            user = users[sp.authorId] ?: User(),
                            content = sp.contentList.toPostContent(),
                            floor = sp.floor,
                            postId = p.id,
                            tid = response.thread.id,
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
                        p.subPostNumber,
                        agreeNum = p.agree.agreeNum,
                        disagreeNum = p.agree.disagreeNum,
                        page = currentPage
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
                val prevKey: Int?
                val nextKey: Int?
                if (threadConfig.reverse) {
                    prevKey =
                        if (response.page.hasPrev != 0) currentPage + 1 else null
                    nextKey =
                        if (response.page.hasMore != 0) currentPage - 1 else null
                } else {
                    prevKey =
                        if (response.page.hasPrev != 0) currentPage - 1 else null
                    nextKey =
                        if (response.page.hasMore != 0) currentPage + 1 else null
                }
                return LoadResult.Page(
                    data = posts,
                    prevKey = prevKey,
                    nextKey = nextKey
                )
            } catch (t: Throwable) {
                Logger.e("failed to load thread $key $threadConfig", t)
                LoadResult.Error(t)
            }
        }

        override fun getRefreshKey(state: PagingState<Int, Post>): Int? {
            return null
        }
    }

    val flow = Pager(
        PagingConfig(pageSize = 30)
    ) {
        photos.clear()
        PostPagingSource(App.instance.client, threadConfig)
    }.flow.map {
        it.map<Post, PostModel> { item -> PostModel.Post(item) }
            .insertHeaderItem(item = PostModel.Header)
    }.cachedIn(viewModelScope)
}