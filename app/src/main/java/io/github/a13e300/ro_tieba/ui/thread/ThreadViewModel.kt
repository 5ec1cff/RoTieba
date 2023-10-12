package io.github.a13e300.ro_tieba.ui.thread

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
import io.github.a13e300.ro_tieba.cache.CachedThread
import io.github.a13e300.ro_tieba.models.Content
import io.github.a13e300.ro_tieba.models.Photo
import io.github.a13e300.ro_tieba.models.Post
import io.github.a13e300.ro_tieba.ui.photo.toPhoto
import kotlinx.coroutines.flow.map
import java.util.TreeMap


data class ThreadConfig(
    val tid: Long,
    val pid: Long = 0L,
    val reverse: Boolean = false,
    val page: Int = 0,
    val seeLz: Boolean = false
)

class ThreadViewModel : ViewModel() {
    sealed class ScrollRequest {
        abstract val offset: Int
        abstract val highlight: Boolean

        data class ByPid(
            val pid: Long, override val offset: Int = 0,
            override val highlight: Boolean = true
        ) : ScrollRequest()

        data class ByFloor(
            val floor: Int, override val offset: Int = 0,
            override val highlight: Boolean = true
        ) : ScrollRequest()

        data class ByPage(
            val page: Int, override val offset: Int = 0,
            override val highlight: Boolean = false
        ) : ScrollRequest()
    }

    var scrollRequest: ScrollRequest? = null
    val totalPage
        get() = mCachedThread.getTotalPage(threadConfig.seeLz) ?: 0
    var currentUid: String? = null
    lateinit var threadConfig: ThreadConfig
    private lateinit var mCachedThread: CachedThread

    var historyAdded: Boolean = false

    // TODO: use SavedStateHandle
    var initialized: Boolean = false
    val threadInfo
        get() = mCachedThread.threadInfo
    val photos = TreeMap<Pair<Int, Int>, Photo> { p0, p1 ->
        if (p0.first < p1.first) -1
        else if (p0.first > p1.first) 1
        else if (p0.second < p1.second) -1
        else if (p0.second > p1.second) 1
        else 0
    }

    fun init() {
        if (initialized) return
        mCachedThread = CachedThread.obtain(threadConfig.tid)
        initialized = true
    }

    override fun onCleared() {
        super.onCleared()
        if (initialized) {
            CachedThread.release(threadConfig.tid)
        }
    }

    fun invalidateCache() {
        if (initialized) mCachedThread.invalidateCache()
    }

    sealed class PostModel {
        data class Post(val post: io.github.a13e300.ro_tieba.models.Post) : PostModel()
        data object Header : PostModel()
    }

    inner class PostPagingSource(
        private val client: TiebaClient,
        private val threadConfig: ThreadConfig
    ) : PagingSource<Int, Post>() {

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Post> {
            val key = params.key
            return try {
                val result = if (key != null) mCachedThread.loadPage(
                    key,
                    threadConfig.seeLz,
                    threadConfig.reverse
                )
                else if (threadConfig.pid != 0L) mCachedThread.loadPageByPid(
                    threadConfig.pid,
                    threadConfig.seeLz,
                    threadConfig.reverse
                )
                else mCachedThread.loadPage(
                    threadConfig.page,
                    threadConfig.seeLz,
                    threadConfig.reverse
                )
                result.posts.forEach { p ->
                    p.content.forEach { c ->
                        if (c is Content.ImageContent) {
                            photos[p.floor to c.order] = c.toPhoto(p)
                        }
                    }
                }
                val prevKey: Int?
                val nextKey: Int?
                if (threadConfig.reverse) {
                    prevKey =
                        if (result.hasPrev) result.page + 1 else null
                    nextKey =
                        if (result.hasNext) result.page - 1 else null
                } else {
                    prevKey =
                        if (result.hasPrev) result.page - 1 else null
                    nextKey =
                        if (result.hasNext) result.page + 1 else null
                }
                return LoadResult.Page(
                    data = result.posts,
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