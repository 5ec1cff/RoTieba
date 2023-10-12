package io.github.a13e300.ro_tieba.cache

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.Logger
import io.github.a13e300.ro_tieba.api.TiebaApiError
import io.github.a13e300.ro_tieba.models.Comment
import io.github.a13e300.ro_tieba.models.Forum
import io.github.a13e300.ro_tieba.models.Post
import io.github.a13e300.ro_tieba.models.TiebaThread
import io.github.a13e300.ro_tieba.models.User
import io.github.a13e300.ro_tieba.models.toUser
import io.github.a13e300.ro_tieba.utils.toPostContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tbclient.PbPage.PbPageResIdlOuterClass
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min

class CachedThread private constructor(val tid: Long) {
    private val mRef = AtomicInteger(0)

    companion object {
        const val FIRST_PAGE = 0

        private val sActiveCachedThreads = mutableMapOf<Long, CachedThread>()
        private var sInactiveCachedThread: CachedThread? = null

        @Synchronized
        fun obtain(tid: Long): CachedThread {
            sInactiveCachedThread?.let {
                if (it.tid == tid) {
                    sInactiveCachedThread = null
                    sActiveCachedThreads[tid] = it
                    it.mRef.incrementAndGet()
                    return it
                }
            }
            return sActiveCachedThreads.computeIfAbsent(tid) {
                CachedThread(tid).also { it.mRef.incrementAndGet() }
            }
        }

        @Synchronized
        fun release(tid: Long) {
            val t = sActiveCachedThreads[tid] ?: return
            if (t.mRef.decrementAndGet() == 0) {
                sActiveCachedThreads.remove(tid)
                sInactiveCachedThread = t
            }
        }
    }

    private val mThreadInfo = MutableLiveData<TiebaThread>()
    private val mPageNoSeeLz = MutableLiveData<Int>()
    private val mPageWithSeeLz = MutableLiveData<Int>()
    private val mPageCache = ConcurrentHashMap<PageCacheKey, PageCache>()
    private var mNeedInvalidate = AtomicBoolean(false)

    fun getTotalPage(seeLz: Boolean) = if (seeLz) mPageWithSeeLz.value else mPageNoSeeLz.value
    val threadInfo
        get() = mThreadInfo as LiveData<TiebaThread>

    fun invalidateCache() {
        mNeedInvalidate.set(true)
    }

    private data class PageCacheKey(
        val page: Int,
        val seeLz: Boolean
    )

    private data class PageCache(
        val posts: List<Post>,
        val hasPrev: Boolean,
        val hasNext: Boolean,
        val reverse: Boolean
    )

    data class LoadResult(
        val posts: List<Post>,
        internal val totalPage: Int,
        val page: Int,
        val hasPrev: Boolean,
        val hasNext: Boolean
    )

    suspend fun loadPage(page: Int, seeLz: Boolean, reverse: Boolean): LoadResult {
        require(page >= 0) { "page is less than 0" }
        return withContext(Dispatchers.IO) {
            val totalPageLiveData = if (seeLz) mPageWithSeeLz else mPageNoSeeLz
            var totalPage: Int? = totalPageLiveData.value
            if (totalPage == null) {
                totalPage = fetchAndCache(page, seeLz, reverse, false).totalPage
            }
            val realPage = if (page == FIRST_PAGE) {
                if (reverse) totalPage else 1
            } else page
            fetchAndCache(realPage, seeLz, reverse, true)
        }
    }

    suspend fun loadPageByPid(pid: Long, seeLz: Boolean, reverse: Boolean): LoadResult {
        require(pid >= 0) { "pid is no more than 0" }
        return withContext(Dispatchers.IO) {
            var (pageForPid, totalPage) = queryPidPage(pid, seeLz, reverse)
            if (totalPage == 0 && reverse) {
                // unluckily, the pid does not exists, and user requested reversed order,
                // so we query the last page
                // this may not happen because there is no way to open a thread
                // with specified pid and reversed order (for now)
                totalPage = fetchAndCache(0, seeLz, reverse = false, useCache = false).totalPage
            }
            val realPage = if (pageForPid == FIRST_PAGE) {
                if (reverse) totalPage else 1
            } else pageForPid
            fetchAndCache(realPage, seeLz, reverse, true)
        }
    }

    private suspend fun queryPidPage(pid: Long, seeLz: Boolean, reverse: Boolean): Pair<Int, Int> {
        val totalPageLiveData = if (seeLz) mPageWithSeeLz else mPageNoSeeLz
        var totalPage = totalPageLiveData.value
        if (totalPage != null) {
            val localPage = mPageCache.firstNotNullOfOrNull { (k, v) ->
                val firstPid = v.posts.first().postId
                val lastPid = v.posts.last().postId
                if (k.seeLz == seeLz && pid in min(firstPid, lastPid)..max(
                        firstPid,
                        lastPid
                    )
                ) v.posts else null
            }?.firstNotNullOfOrNull { if (it.postId == pid) it.page else null }
            if (localPage != null) {
                Logger.d("query pid page from local cache: pid=$pid -> page=$localPage")
                return Pair(localPage, totalPage)
            }
        }
        // pid-page-query does not return a normal page, so we don't need to cache
        try {
            val response = App.instance.client.getPosts(
                tid, page = 0, pid = pid,
                sort = if (reverse) 1 else 0, seeLz = seeLz
            )
            totalPage = response.page.totalPage
            totalPageLiveData.postValue(totalPage)
            Logger.d("query pid page from remote: pid=$pid -> page=${response.page.currentPage}")
            return Pair(response.page.currentPage, totalPage)
        } catch (e: TiebaApiError) {
            Logger.e("failed to query page for $pid in $tid", e)
        } catch (t: Throwable) {
            throw t
        }
        return Pair(FIRST_PAGE, 0)
    }

    private suspend fun fetchAndCache(
        page: Int,
        seeLz: Boolean,
        reverse: Boolean,
        useCache: Boolean
    ): LoadResult {
        if (useCache && page > 0) {
            if (mNeedInvalidate.getAndSet(false)) {
                Logger.d("invalidate requested")
                mPageCache.clear()
            }
            mPageCache[PageCacheKey(page, seeLz)]?.let { cached ->
                Logger.d("use cached: tid=$tid page=$page seeLz=$seeLz reverse=$reverse")
                return LoadResult(
                    posts = if (reverse xor cached.reverse) cached.posts.asReversed() else cached.posts,
                    totalPage = 0, // from cache
                    hasNext = if (reverse xor cached.reverse) cached.hasPrev else cached.hasNext,
                    hasPrev = if (reverse xor cached.reverse) cached.hasNext else cached.hasPrev,
                    page = page
                )
            }
        }
        val response = App.instance.client.getPosts(
            tid, page = page,
            sort = if (reverse) 1 else 0, seeLz = seeLz
        )
        updateThreadInfo(response)
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
                    ppid = sp.id
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
        val totalPage = response.page.totalPage
        val totalPageLiveData = if (seeLz) mPageWithSeeLz else mPageNoSeeLz
        totalPageLiveData.postValue(totalPage)
        if ((page != 0 || !reverse) // reverse + page 0 does not return a normal page
        // && (currentPage != totalPage) // we don't cache the last page
        ) {
            Logger.d("cached: tid=$tid page=$page seeLz=$seeLz reverse=$reverse totalPage=$totalPage")
            mPageCache[PageCacheKey(currentPage, seeLz)] = PageCache(
                posts = posts,
                reverse = reverse,
                hasPrev = response.page.hasPrev != 0,
                hasNext = response.page.hasMore != 0
            )
        }
        return LoadResult(
            posts = posts,
            totalPage = totalPage,
            page = currentPage,
            hasPrev = response.page.hasPrev != 0,
            hasNext = response.page.hasMore != 0
        )
    }

    private fun updateThreadInfo(response: PbPageResIdlOuterClass.PbPageResIdl.DataRes) {
        mThreadInfo.postValue(
            TiebaThread(
                tid = response.thread.id,
                title = response.thread.title,
                author = response.thread.author.toUser(),
                content = listOf(),
                replyNum = response.thread.replyNum,
                time = Date(response.thread.createTime.toLong() * 1000), // TODO: remove this useless date
                postId = response.thread.postId,
                isGood = response.thread.isGood == 1,
                forum = Forum(
                    response.forum.name,
                    response.forum.id,
                    avatarUrl = response.forum.avatar
                ),
                createTime = Date(response.thread.createTime.toLong() * 1000),
                agreeNum = response.thread.agree.agreeNum,
                disagreeNum = response.thread.agree.disagreeNum
            )
        )
    }

}
