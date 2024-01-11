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
import io.github.a13e300.ro_tieba.arch.Event
import io.github.a13e300.ro_tieba.datastore.SearchHistory
import io.github.a13e300.ro_tieba.models.Forum
import io.github.a13e300.ro_tieba.models.PostId
import io.github.a13e300.ro_tieba.models.SearchedPost
import io.github.a13e300.ro_tieba.models.User
import io.github.a13e300.ro_tieba.models.toForum
import io.github.a13e300.ro_tieba.models.toUser
import io.github.a13e300.ro_tieba.utils.htmlToContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

sealed class Operation {
    data class History(val entry: SearchHistory.Entry) : Operation()
    data object RemoveHistories : Operation()
    data class GoToForum(val name: String) : Operation()
    data class GoToThread(val id: PostId, val fromClip: Boolean = false) : Operation()

    data class GoToUser(val uidOrPortrait: String) : Operation()
    data class SearchForum(val name: String) : Operation()
    data class SearchPosts(val keyword: String) : Operation()
    data class SearchUsers(val keyword: String) : Operation()
}

sealed class SearchState<out T> {
    data object Uninitialized : SearchState<Nothing>()
    data object Fetching : SearchState<Nothing>()
    data class Result<T>(val data: T) : SearchState<T>()
    data class Error<T>(val error: Throwable) : SearchState<T>()
}

class SearchViewModel : ViewModel() {
    var initialized = false
    lateinit var forum: String
    var searchAtForum = false
    var currentKeyword: String = ""
    var postKeyWord: String = currentKeyword
    var searchPostFilter: SearchFilter = SearchFilter.ALL
    val searchPostOrder = MutableLiveData(SearchOrder.NEW)
    var needShowSearch = true
    val searchPostEvent = MutableLiveData<Event<String>>()

    // search forums
    val searchForumEvent = MutableLiveData<Event<String>>()
    var searchedForums = MutableLiveData<SearchState<List<Forum>>>(SearchState.Uninitialized)
    val forumCount = MutableLiveData<Int?>(null)
    private var searchForumJob: Job? = null

    // search users
    val searchUserEvent = MutableLiveData<Event<String>>()
    var searchedUsers = MutableLiveData<SearchState<List<User>>>(SearchState.Uninitialized)
    val userCount = MutableLiveData<Int?>(null)
    private var searchUserJob: Job? = null

    fun fetchForums(keyword: String) {
        searchForumJob?.cancel()
        searchedForums.value = SearchState.Fetching
        searchForumJob = viewModelScope.launch {
            val list = mutableListOf<Forum>()
            kotlin.runCatching {
                withContext(Dispatchers.IO) {
                    val r = App.instance.client.webAPI.searchForum(keyword)
                    r.exactMatch?.toForum()?.let { list.add(it) }
                    r.fuzzyMatch.forEach { list.add(it.toForum()) }
                }
                searchedForums.value = SearchState.Result(list)
                forumCount.value = list.size
            }.onFailure {
                if (it !is CancellationException) {
                    Logger.e("failed to search forum $keyword", it)
                    searchedForums.value = SearchState.Error(it)
                    forumCount.value = null
                }
            }
        }
    }


    fun fetchUsers(keyword: String) {
        searchUserJob?.cancel()
        searchedUsers.value = SearchState.Fetching
        searchUserJob = viewModelScope.launch {
            val list = mutableListOf<User>()
            kotlin.runCatching {
                withContext(Dispatchers.IO) {
                    val r = App.instance.client.webAPI.searchUser(keyword)
                    r.exactMatch?.toUser()?.let { list.add(it) }
                    r.fuzzyMatch.forEach { list.add(it.toUser()) }
                }
                searchedUsers.value = SearchState.Result(list)
                userCount.value = list.size
            }.onFailure {
                if (it !is CancellationException) {
                    Logger.e("failed to search user $keyword", it)
                    searchedUsers.value = SearchState.Error(it)
                    userCount.value = null
                }
            }
        }
    }

    inner class PostPagingSource(
        private val client: TiebaClient,
        private val keyword: String
    ) : PagingSource<Int, SearchedPost>() {
        private suspend fun searchThread(page: Int) = client.webAPI.searchThread(
            keyword,
            page,
            searchPostOrder.value!!.value,
            searchPostFilter.value
        ).let {
            it.hasMore to it.postList.map { p ->
                SearchedPost(
                    user = User(
                        name = p.user.userName ?: "null",
                        nick = p.user.showNickname,
                        avatar = p.user.portrait,
                        uid = p.user.userId?.toLong() ?: 0
                    ),
                    id = when (p.type) {
                        3 -> PostId.Comment(p.tid.toLong(), p.pid.toLong(), p.cid.toLong())
                        else -> PostId.Post(p.tid.toLong(), p.pid.toLong())
                    },
                    time = Date(p.time.toLong() * 1000),
                    title = p.title,
                    forum = Forum(p.forumName, p.forumId, p.forumInfo.avatar),
                    content = p.content.htmlToContent()
                )
            }
        }

        private suspend fun searchForumPost(page: Int) =
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
                        user = User(
                            name = p.author.name ?: "null",
                            nick = p.author.showName,
                            avatar = "",
                            uid = 0
                        ),
                        id = if (p.cid != "0") {
                            PostId.Comment(p.tid.toLong(), p.pid.toLong(), p.cid.toLong())
                        } else PostId.Post(p.tid.toLong(), p.pid.toLong()),
                        time = Date(p.time.toLong() * 1000),
                        title = p.title,
                        forum = Forum(p.forumName, 0),
                        content = p.content.htmlToContent()
                    )
                }
            }

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchedPost> {
            if (keyword.isEmpty()) return LoadResult.Page(emptyList(), null, null)
            try {
                val page = params.key ?: 1
                if (searchAtForum) {
                    val (hasMore, posts) = searchForumPost(page)
                    return LoadResult.Page(
                        data = posts,
                        prevKey = if (page == 1) null else (page - 1),
                        nextKey = if (hasMore) page + 1 else null
                    )

                } else {
                    val (hasMore, posts) = searchThread(page)
                    return LoadResult.Page(
                        data = posts,
                        prevKey = if (page == 1) null else (page - 1),
                        nextKey = if (hasMore) page + 1 else null
                    )
                }
            } catch (t: Throwable) {
                if (t !is CancellationException)
                    Logger.e("failed to search $keyword ${params.key}", t)
                else
                    Logger.e("search $keyword ${params.key} has been cancelled")
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
        PostPagingSource(App.instance.client, postKeyWord)
    }.flow
        .cachedIn(viewModelScope)
}