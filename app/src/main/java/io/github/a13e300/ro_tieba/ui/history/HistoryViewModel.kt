package io.github.a13e300.ro_tieba.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import io.github.a13e300.ro_tieba.App

class HistoryViewModel : ViewModel() {
    val flow = Pager(
        PagingConfig(pageSize = 30)
    ) {
        App.instance.db.historyDao().getHistories()
    }.flow
        .cachedIn(viewModelScope)
}