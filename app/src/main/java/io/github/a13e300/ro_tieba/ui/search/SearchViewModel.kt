package io.github.a13e300.ro_tieba.ui.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.Logger
import io.github.a13e300.ro_tieba.api.web.SearchForum
import io.github.a13e300.ro_tieba.models.Forum
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

fun SearchForum.ForumInfo.toForum(): Forum = Forum(forumName, forumId.toLong(), avatar, intro)

class SearchViewModel : ViewModel() {
    var needShowSearch = true
    val searchedBars = MutableLiveData<List<Forum>>()
    val barLoadState = MutableLiveData<LoadState>()
    var suggestions: List<Operation> = emptyList()
    private var searchBarJob: Job? = null

    fun fetchBars(keyword: String) {
        searchBarJob?.cancel()
        barLoadState.value = LoadState.FETCHING
        searchBarJob = viewModelScope.launch {
            Logger.d("search $keyword")
            val list = mutableListOf<Forum>()
            withContext(Dispatchers.IO) {
                val r = App.instance.client.webAPI.searchForum(keyword)
                r.exactMatch?.toForum()?.let { list.add(it) }
                r.fuzzyMatch.forEach { list.add(it.toForum()) }
            }
            searchedBars.value = list
            barLoadState.value = LoadState.FETCHED
        }
    }
}