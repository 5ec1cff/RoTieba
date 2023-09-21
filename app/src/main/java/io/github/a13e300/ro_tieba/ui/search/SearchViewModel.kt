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
import io.github.a13e300.ro_tieba.api.web.SearchOrder
import io.github.a13e300.ro_tieba.models.Forum
import io.github.a13e300.ro_tieba.models.Post
import io.github.a13e300.ro_tieba.models.SearchedPost
import io.github.a13e300.ro_tieba.models.User
import io.github.a13e300.ro_tieba.models.toForum
import io.github.a13e300.ro_tieba.models.toUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

sealed class Operation {
    data class GoToForum(val name: String) : Operation()
    data class GoToThread(val tid: Long, val pid: Long = 0) : Operation()

    data class GoToUser(val uidOrPortrait: String) : Operation()
    data class SearchForum(val name: String) : Operation()
    data class SearchPosts(val keyword: String) : Operation()
    data class SearchUsers(val keyword: String) : Operation()
}

enum class LoadState {
    LOADED, FETCHING, FETCHED
}

sealed class SearchResult<T> {
    data class Result<T>(val data: T) : SearchResult<T>()
    data class Error<T>(val error: Throwable) : SearchResult<T>()
}

class SearchViewModel : ViewModel() {
    var initialized = false
    lateinit var forum: String
    var searchAtForum = false
    val currentKeyword = MutableLiveData<String>()
    var searchPostKeyWord: String? = null
    var searchPostFilter: SearchFilter = SearchFilter.ALL
    val searchPostOrder = MutableLiveData(SearchOrder.NEW)
    var needShowSearch = true

    // search forums
    var forumSearched = false
    var searchedForums: SearchResult<List<Forum>> = SearchResult.Result(emptyList())
    val forumLoadState = MutableLiveData<LoadState>()
    private var searchForumJob: Job? = null

    // search users
    var userSearched = false
    var searchedUsers: SearchResult<List<User>> = SearchResult.Result(emptyList())
    val userLoadState = MutableLiveData<LoadState>()
    private var searchUserJob: Job? = null

    var suggestions: List<Operation> = emptyList()


    fun fetchForums(keyword: String) {
        forumSearched = true
        searchForumJob?.cancel()
        forumLoadState.value = LoadState.FETCHING
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
            forumLoadState.value = LoadState.FETCHED
        }
    }


    fun fetchUsers(keyword: String) {
        userSearched = true
        searchUserJob?.cancel()
        userLoadState.value = LoadState.FETCHING
        searchUserJob = viewModelScope.launch {
            val list = mutableListOf<User>()
            kotlin.runCatching {
                withContext(Dispatchers.IO) {
                    val r = App.instance.client.webAPI.searchUser(keyword)
                    r.exactMatch?.toUser()?.let { list.add(it) }
                    r.fuzzyMatch.forEach { list.add(it.toUser()) }
                }
                searchedUsers = SearchResult.Result(list)
            }.onFailure {
                if (it !is CancellationException) {
                    Logger.e("failed to search user $keyword", it)
                    searchedUsers = SearchResult.Error(it)
                }
            }
            userLoadState.value = LoadState.FETCHED
        }
    }

    inner class PostPagingSource(
        private val client: TiebaClient
    ) : PagingSource<Int, SearchedPost>() {
        private suspend fun searchThread(keyword: String, page: Int) = client.webAPI.searchThread(
            keyword,
            page,
            searchPostOrder.value!!.value,
            searchPostFilter.value
        ).let {
            it.hasMore to it.postList.map { p ->
                SearchedPost(
                    post = Post(
                        user = User(
                            name = p.user.userName ?: "null",
                            nick = p.user.showNickname,
                            avatar = p.user.portrait,
                            uid = p.user.userId?.toLong() ?: 0
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
        }

        private suspend fun searchForumPost(keyword: String, page: Int) =
            client.jsonAPI.searchForumPost(
                forum,
                keyword,
                if (searchPostFilter == SearchFilter.ALL) "0" else "1",
                page,
                30,
                searchPostOrder.value!!.value
            ).let {
                it.page.hasMore to it.postList.map { p ->
                    SearchedPost(
                        post = Post(
                            user = User(
                                name = p.author.name ?: "null",
                                nick = p.author.showName,
                                avatar = "",
                                uid = 0
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
            }

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchedPost> {
            val keyword = currentKeyword.value ?: return LoadResult.Page(emptyList(), null, null)
            try {
                val page = params.key ?: 1
                if (searchAtForum) {
                    val (hasMore, posts) = searchForumPost(keyword, page)
                    searchPostKeyWord = keyword
                    return LoadResult.Page(
                        data = posts,
                        prevKey = if (page == 1) null else (page - 1),
                        nextKey = if (hasMore) page + 1 else null
                    )

                } else {
                    val (hasMore, posts) = searchThread(keyword, page)
                    searchPostKeyWord = keyword
                    return LoadResult.Page(
                        data = posts,
                        prevKey = if (page == 1) null else (page - 1),
                        nextKey = if (hasMore) page + 1 else null
                    )
                }
            } catch (t: Throwable) {
                if (t !is CancellationException)
                    Logger.e("failed to search $keyword ${params.key}", t)
                return LoadResult.Error(t)
            }
        }

        override fun getRefreshKey(state: PagingState<Int, SearchedPost>): Int {
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