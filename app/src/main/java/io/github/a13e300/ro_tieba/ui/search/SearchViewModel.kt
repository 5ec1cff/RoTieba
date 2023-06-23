package io.github.a13e300.ro_tieba.ui.search

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
import io.github.a13e300.ro_tieba.api.web.SearchFilter
import io.github.a13e300.ro_tieba.api.web.SearchForum
import io.github.a13e300.ro_tieba.api.web.SearchOrder
import io.github.a13e300.ro_tieba.models.Forum
import io.github.a13e300.ro_tieba.models.Post
import io.github.a13e300.ro_tieba.models.SearchedPost
import io.github.a13e300.ro_tieba.models.User
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

sealed class Operation {
    data class GoToForum(val name: String) : Operation()
    data class GoToThread(val tid: Long) : Operation()

    // data class GoToUser
    data class SearchForum(val name: String) : Operation()
    data class SearchPosts(val keyword: String) : Operation()
    // data class SearchUsers
}

enum class LoadState {
    LOADED, FETCHING, FETCHED
}

sealed class SearchResult<T> {
    data class Result<T>(val data: T) : SearchResult<T>()
    data class Error<T>(val error: Throwable) : SearchResult<T>()
}

fun SearchForum.ForumInfo.toForum(): Forum = Forum(forumName, forumId.toLong(), avatar, slogan)

class SearchViewModel : ViewModel() {
    val currentKeyword = MutableLiveData<String>()
    var searchPostKeyWord: String? = null
    var searchPostFilter: SearchFilter = SearchFilter.ALL
    val searchPostOrder = MutableLiveData(SearchOrder.NEW)
    var searched = false
    var needShowSearch = true
    var searchedForums: SearchResult<List<Forum>> = SearchResult.Result(emptyList())
    val barLoadState = MutableLiveData<LoadState>()
    var suggestions: List<Operation> = emptyList()
    private var searchForumJob: Job? = null


    fun fetchForums(keyword: String) {
        searched = true
        searchForumJob?.cancel()
        barLoadState.value = LoadState.FETCHING
        searchForumJob = viewModelScope.launch {
            val list = mutableListOf<Forum>()
            kotlin.runCatching {
                withContext(Dispatchers.IO) {
                    val r = App.instance.client.webAPI.searchForum(keyword)
                    r.exactMatch?.toForum()?.let { list.add(it) }
                    r.fuzzyMatch.forEach { list.add(it.toForum()) }
                }
                searchedForums = SearchResult.Result(list)
            }.onFailure {
                if (it !is CancellationException) {
                    Logger.e("failed to search forum $keyword", it)
                    searchedForums = SearchResult.Error(it)
                }
            }
            barLoadState.value = LoadState.FETCHED
        }
    }

    inner class PostPagingSource(
        private val client: TiebaClient
    ) : PagingSource<Int, SearchedPost>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchedPost> {
            val keyword = currentKeyword.value ?: return LoadResult.Page(emptyList(), null, null)
            try {
                val page = params.key ?: 1
                val response = client.webAPI.searchThread(
                    keyword,
                    page,
                    searchPostOrder.value!!.value,
                    searchPostFilter.value
                )
                val posts = response.postList.map { p ->
                    SearchedPost(
                        post = Post(
                            user = User(
                                name = p.user.userName,
                                nick = p.user.showNickname,
                                avatar = p.user.portrait,
                                uid = p.user.userId.toLong()
                            ),
                            content = emptyList(),
                            floor = 0, // unknown
                            postId = p.pid.toLong(),
                            tid = p.tid.toLong(),
                            time = Date(p.time.toLong() * 1000),
                            comments = emptyList(),
                            commentCount = 0,
                        ),
                        title = p.title,
                        forum = p.forumName,
                        content = p.content
                    )
                }
                searchPostKeyWord = keyword
                return LoadResult.Page(
                    data = posts,
                    prevKey = if (page == 1) null else (page - 1),
                    nextKey = if (response.hasMore) page + 1 else null
                )
            } catch (t: Throwable) {
                return LoadResult.Error(t)
            }
        }

        override fun getRefreshKey(state: PagingState<Int, SearchedPost>): Int? {
            return 1
        }
    }

    val flow = Pager(
        PagingConfig(pageSize = 30)
    ) {
        PostPagingSource(App.instance.client)
    }.flow
        .cachedIn(viewModelScope)
}