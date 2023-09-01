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
import io.github.a13e300.ro_tieba.toPostContent
import io.github.a13e300.ro_tieba.ui.photo.Photo
import kotlinx.coroutines.flow.map
import java.util.Date
import java.util.TreeMap


data class ThreadConfig(
    val tid: Long,
    val pid: Long = 0L,
    val reverse: Boolean = false,
    val page: Int = 0
)

class ThreadViewModel : ViewModel() {
    var totalPage = 0 // FIXME: request totalPage
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

    sealed class Key
    data class PageKey(val pn: Int) : Key()
    data class PidKey(
        val pid: Long,
        val reverse: Boolean
    ) : Key()

    sealed class PostModel {
        data class Post(val post: io.github.a13e300.ro_tieba.models.Post) : PostModel()
        data object Header : PostModel()
    }

    inner class PostPagingSource(
        private val client: TiebaClient,
        private val threadConfig: ThreadConfig
    ) : PagingSource<Key, Post>() {
        override suspend fun load(params: LoadParams<Key>): LoadResult<Key, Post> {
            try {
                val pid = threadConfig.pid
                val key = params.key ?: if (pid != 0L) {
                    PidKey(pid, false)
                } else if (threadConfig.reverse) PageKey(totalPage) else PageKey(1)
                // Logger.d("load thread $key $threadConfig")
                val response = try {
                    when (key) {
                        is PageKey -> client.getPosts(
                            threadConfig.tid,
                            key.pn,
                            sort = if (threadConfig.reverse) 1 else 0
                        )
                        is PidKey -> client.getPosts(
                            threadConfig.tid,
                            if (params.key == null) 0 else 1,
                            key.pid, sort = if (key.reverse xor threadConfig.reverse) 1 else 0
                        )
                    }
                } catch (t: Throwable) {
                    Logger.e("load thread $key", t)
                    throw t
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
                totalPage = response.page.totalPage
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
                }.let { if (key is PidKey && key.reverse) it.reversed() else it }
                posts.forEach { p ->
                    p.content.forEach { c ->
                        if (c is Content.ImageContent) {
                            photos[p.floor to c.order] =
                                Photo(c.src, c.order, p)
                        }
                    }
                }
                val prevKey: Key?
                val nextKey: Key?
                when (key) {
                    is PageKey -> {
                        if (threadConfig.reverse) {
                            val currentPage =
                                // if (key.pn == 0) response.page.totalPage // use pn = 0 is incorrect
                                response.page.currentPage
                            prevKey =
                                if (response.page.hasPrev != 0) PageKey(currentPage + 1) else null
                            nextKey =
                                if (response.page.hasMore != 0) PageKey(currentPage - 1) else null
                        } else {
                            val currentPage = response.page.currentPage
                            prevKey =
                                if (response.page.hasPrev != 0) PageKey(currentPage - 1) else null
                            nextKey =
                                if (response.page.hasMore != 0) PageKey(currentPage + 1) else null
                        }
                        // Logger.d("$key prev=$prevKey next=$nextKey currentPage=${response.page.currentPage} totalPage=${response.page.totalPage} ${response.postListList.first().floor}..${response.postListList.last().floor}")
                    }

                    is PidKey -> {
                        if (threadConfig.reverse) {
                            if (params.key == null) {
                                prevKey = PidKey(key.pid, true)
                                nextKey =
                                    if (response.postListList.last().floor == 1) null else PidKey(
                                        response.postListList.last().id,
                                        false
                                    )
                            } else if (key.reverse) {
                                prevKey = if (response.postListList.isEmpty()) null else PidKey(
                                    response.postListList.last().id,
                                    true
                                )
                                nextKey = null
                            } else {
                                nextKey =
                                    if (response.postListList.last().floor == 1) null else PidKey(
                                        response.postListList.last().id,
                                        false
                                    )
                                prevKey = null
                            }
                        } else {
                            if (params.key == null) {
                                prevKey =
                                    if (response.postListList.first().floor == 1) null else PidKey(
                                        key.pid,
                                        true
                                    )
                                nextKey = if (response.page.hasMore == 0) null else PidKey(
                                    response.postListList.last().id,
                                    false
                                )
                            } else if (key.reverse) {
                                val first = response.postListList.last()
                                prevKey = if (first.floor == 1) null else PidKey(first.id, true)
                                nextKey = null
                            } else {
                                nextKey = if (response.postListList.isEmpty()) null else PidKey(
                                    response.postListList.last().id,
                                    false
                                )
                                prevKey = null
                            }
                        }
                    }
                }
                return LoadResult.Page(
                    data = posts,
                    prevKey = prevKey,
                    nextKey = nextKey
                )
            } catch (t: Throwable) {
                Logger.e("error load thread $threadConfig", t)
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
        PostPagingSource(App.instance.client, threadConfig)
    }.flow.map {
        it.map<Post, PostModel> { item -> PostModel.Post(item) }
            .insertHeaderItem(item = PostModel.Header)
    }.cachedIn(viewModelScope)
}