package io.github.a13e300.ro_tieba.ui.search

import androidx.lifecycle.ViewModel

sealed class Operation {
    data class GoToBar(val name: String) : Operation()
    data class GoToThread(val tid: Long) : Operation()
    // data class GoToUser
    // data class SearchBar
    // data class SearchPosts
    // data class SearchUsers
}

class SearchViewModel : ViewModel() {
    var suggestions: List<Operation> = emptyList()
}