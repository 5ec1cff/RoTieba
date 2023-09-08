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
import io.github.a13e300.ro_tieba.api.TiebaClient
import io.github.a13e300.ro_tieba.models.UserForum
import io.github.a13e300.ro_tieba.models.UserProfile
import io.github.a13e300.ro_tieba.models.toUserForum
import io.github.a13e300.ro_tieba.models.toUserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ProfileViewModel : ViewModel() {
    var uid = 0L
    var followedForumsHidden = false
    val user = MutableLiveData<UserProfile>()
    suspend fun requestUser(uid: Long, portrait: String?) {
        val p = withContext(Dispatchers.IO) {
            App.instance.client.getUserProfile(portrait, uid)
        }
        user.value = p.user.toUserProfile()
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
            val response = client.jsonAPI.getFollowForums(uid.toString(), page, 50)
            val result = mutableListOf<UserForum>()
            response.forumList?.nonGconForum?.also { list -> result.addAll(list.map { it.toUserForum() }) }
            response.forumList?.gconForum?.also { list -> result.addAll(list.map { it.toUserForum() }) }
            followedForumsHidden = response.forumList == null
            return LoadResult.Page(
                data = result,
                prevKey = null,
                nextKey = if (response.hasMore) page + 1 else null
            )
        }

        override fun getRefreshKey(state: PagingState<Int, UserForum>): Int? {
            return state.anchorPosition?.let { anchorPosition ->
                val anchorPage = state.closestPageToPosition(anchorPosition)
                anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
            }
        }
    }

    val flow: Flow<PagingData<UserForum>> = Pager(
        PagingConfig(pageSize = 50)
    ) {
        FollowForumSource(App.instance.client, uid)
    }.flow
        .cachedIn(viewModelScope)
}