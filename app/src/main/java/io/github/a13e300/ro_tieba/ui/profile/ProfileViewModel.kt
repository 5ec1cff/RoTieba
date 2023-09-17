package io.github.a13e300.ro_tieba.ui.profile

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.Logger
import io.github.a13e300.ro_tieba.api.TiebaClient
import io.github.a13e300.ro_tieba.models.Forum
import io.github.a13e300.ro_tieba.models.TiebaThread
import io.github.a13e300.ro_tieba.models.User
import io.github.a13e300.ro_tieba.models.UserForum
import io.github.a13e300.ro_tieba.models.UserProfile
import io.github.a13e300.ro_tieba.models.toUserForum
import io.github.a13e300.ro_tieba.models.toUserProfile
import io.github.a13e300.ro_tieba.utils.toPostContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Date

class ProfileViewModel : ViewModel() {
    var uid = 0L
    var portrait: String? = null
    var followedForumsHidden = false
    val user = MutableLiveData<Result<UserProfile>>()
    private var forumSource: FollowForumSource? = null
    private var threadSource: ThreadPagingSource? = null
    suspend fun requestUser(uid: Long, portrait: String?) {
        user.value = runCatching {
            withContext(Dispatchers.IO) {
                App.instance.client.getUserProfile(portrait, uid).user.toUserProfile()
            }
        }.apply {
            onSuccess { v ->
                // refresh if prev uid is 0 (in case that only portrait provided)
                if (this@ProfileViewModel.uid != v.uid) {
                    this@ProfileViewModel.uid = v.uid
                    forumSource?.invalidate()
                    threadSource?.invalidate()
                }
            }
        }
    }

    inner class FollowForumSource(
        private val client: TiebaClient,
        private val uid: Long
    ) : PagingSource<Int, UserForum>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, UserForum> {
            val page = params.key ?: 1
            if (uid == 0L) {
                return LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)
            }
            return try {
                val response = client.jsonAPI.getFollowForums(uid.toString(), page, 50)
                val result = mutableListOf<UserForum>()
                response.forumList?.nonGconForum?.also { list -> result.addAll(list.map { it.toUserForum() }) }
                response.forumList?.gconForum?.also { list -> result.addAll(list.map { it.toUserForum() }) }
                followedForumsHidden = response.forumList == null
                LoadResult.Page(
                    data = result,
                    prevKey = null,
                    nextKey = if (response.hasMore) page + 1 else null
                )
            } catch (t: Throwable) {
                Logger.e("failed to load followed forums", t)
                LoadResult.Error(t)
            }
        }

        override fun getRefreshKey(state: PagingState<Int, UserForum>): Int? {
            return state.anchorPosition?.let { anchorPosition ->
                val anchorPage = state.closestPageToPosition(anchorPosition)
                anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
            }
        }
    }

    val forumsFlow: Flow<PagingData<UserForum>> = Pager(
        PagingConfig(pageSize = 50)
    ) {
        FollowForumSource(App.instance.client, uid).also { forumSource = it }
    }.flow
        .cachedIn(viewModelScope)

    inner class ThreadPagingSource(
        private val client: TiebaClient,
        private val uid: Long
    ) : PagingSource<Int, TiebaThread>() {
        override fun getRefreshKey(state: PagingState<Int, TiebaThread>): Int? = null

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TiebaThread> {
            val pn = params.key ?: 1
            if (uid == 0L) return LoadResult.Page(emptyList(), null, null)
            try {
                // using portrait may return empty list
                val r = client.getUserProfile(null, uid, pn = pn, page = 0)
                val u = User()
                val l = r.postListList.map { p ->
                    TiebaThread(
                        tid = p.threadId,
                        postId = p.postId,
                        author = u,
                        content = p.firstPostContentList.toPostContent(),
                        isGood = false,
                        title = p.title,
                        replyNum = p.replyNum,
                        time = Date(p.createTime.toLong() * 1000),
                        forum = Forum(p.forumName, p.forumId)
                    )
                }
                return LoadResult.Page(
                    l,
                    prevKey = null,
                    nextKey = if (l.isEmpty()) null else pn + 1
                )
            } catch (t: Throwable) {
                return LoadResult.Error(t)
            }
        }
    }

    val threadsFlow: Flow<PagingData<TiebaThread>> = Pager(
        PagingConfig(pageSize = 60)
    ) {
        ThreadPagingSource(App.instance.client, uid).also { threadSource = it }
    }.flow
        .cachedIn(viewModelScope)
}