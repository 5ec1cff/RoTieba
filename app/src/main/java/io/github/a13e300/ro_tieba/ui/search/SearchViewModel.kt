package io.github.a13e300.ro_tieba.ui.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.Logger
import io.github.a13e300.ro_tieba.api.web.SearchForum
import io.github.a13e300.ro_tieba.models.Forum
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class Operation {
    data class GoToBar(val name: String) : Operation()
    data class GoToThread(val tid: Long) : Operation()
    // data class GoToUser
    // data class SearchBar
    // data class SearchPosts
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
    var searched = false
    var needShowSearch = true
    var searchedForums: SearchResult<List<Forum>> = SearchResult.Result(emptyList())
    val barLoadState = MutableLiveData<LoadState>()
    var suggestions: List<Operation> = emptyList()
    private var searchForumJob: Job? = null

    fun fetchBars(keyword: String) {
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
}