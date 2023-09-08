package io.github.a13e300.ro_tieba.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.account.AccountManager
import io.github.a13e300.ro_tieba.api.TiebaClient
import io.github.a13e300.ro_tieba.api.json.GetFollowForums
import kotlinx.coroutines.flow.Flow

class HomeViewModel : ViewModel() {
    private var currentUid: String? = null

    inner class BarPagingSource(
        private val client: TiebaClient,
        private val uid: String = client.account.uid
    ) : PagingSource<Int, GetFollowForums.Forum>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GetFollowForums.Forum> {
            val page = params.key ?: 1
            if (uid == AccountManager.ACCOUNT_ANONYMOUS) {
                return LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)
            }
            val response = client.jsonAPI.getFollowForums(uid, page, 50)
            val result = mutableListOf<GetFollowForums.Forum>()
            response.forumList?.nonGconForum?.also { result.addAll(it) }
            response.forumList?.gconForum?.also { result.addAll(it) }
            return LoadResult.Page(
                data = result,
                prevKey = null,
                nextKey = if (response.hasMore) page + 1 else null
            )
        }

        override fun getRefreshKey(state: PagingState<Int, GetFollowForums.Forum>): Int? {
            return state.anchorPosition?.let { anchorPosition ->
                val anchorPage = state.closestPageToPosition(anchorPosition)
                anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
            }
        }
    }

    lateinit var flow: Flow<PagingData<GetFollowForums.Forum>>

    fun updateUid(uid: String) {
        if (uid == currentUid) return
        currentUid = uid
        flow = Pager(
            PagingConfig(pageSize = 50)
        ) {
            BarPagingSource(App.instance.client)
        }.flow
            .cachedIn(viewModelScope)
    }

}