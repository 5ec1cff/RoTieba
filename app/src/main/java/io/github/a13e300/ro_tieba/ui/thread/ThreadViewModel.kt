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


data class ThreadConfig(
    val tid: Long,
    val pid: Long
)

class ThreadViewModel : ViewModel() {
    var currentUid: String? = null
    val needLoadPrevious = MutableLiveData<Boolean>()
    val threadConfig = MutableLiveData<ThreadConfig>()
    val threadInfo = MutableLiveData<TiebaThread>()
    val photos = TreeMap<Pair<Int, Int>, Photo> { p0, p1 ->
        if (p0.first < p1.first) -1
        else if (p0.first > p1.first) 1
        else if (p0.second < p1.second) -1
        else if (p0.second > p1.second) 1
        else 0
    }

    sealed class Key
    data class PageKey(val pn: Int) : Key()
    data class PidKey(
        val pid: Long,
        val prevPid: Long?
    ) : Key()

    inner class PostPagingSource(
        private val client: TiebaClient
    ) : PagingSource<Key, Post>() {
        override suspend fun load(params: LoadParams<Key>): LoadResult<Key, Post> {
            try {
                val pid = threadConfig.value!!.pid
                val key = params.key ?: if (pid != 0L) {
                    PidKey(pid, null)
                } else PageKey(1)
                val response = when (key) {
                    is PageKey -> client.getPosts(threadConfig.value!!.tid, key.pn)
                    is PidKey -> client.getPosts(
                        threadConfig.value!!.tid,
                        if (key.pid == pid) 0 else 1,
                        key.pid
                    )
                }
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
                        disagreeNum = p.agree.disagreeNum
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
                if (params.key == null) // initial load
                    needLoadPrevious.value =
                        key is PidKey && response.postListList.first().floor != 1
                val prevKey: Key?
                val nextKey: Key?
                when (key) {
                    is PageKey -> {
                        prevKey =
                            if (response.page.hasPrev != 0) PageKey(response.page.currentPage - 1) else null
                        nextKey =
                            if (response.page.hasMore != 0) PageKey(response.page.currentPage + 1) else null
                    }

                    is PidKey -> {
                        prevKey = null
                        if (response.page.hasMore == 0) nextKey = null
                        else {
                            val lastPid = response.postListList.last().id
                            nextKey = PidKey(lastPid, key.pid)
                        }
                    }
                }
                return LoadResult.Page(
                    data = posts,
                    prevKey = prevKey,
                    nextKey = nextKey
                )
            } catch (t: Throwable) {
                return LoadResult.Error(t)
            }
        }

        override fun getRefreshKey(state: PagingState<Key, Post>): Key? {
            return null
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