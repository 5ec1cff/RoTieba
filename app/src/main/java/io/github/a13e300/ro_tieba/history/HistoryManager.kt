package io.github.a13e300.ro_tieba.history

import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.datastore.SearchHistory
import io.github.a13e300.ro_tieba.db.HistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HistoryManager {
    companion object {
        const val MAX_HISTORY_ENTRY_COUNT = 300
        const val MAX_SEARCH_HISTORY_COUNT = 15
    }

    suspend fun addSearch(entry: SearchHistory.Entry) {
        App.instance.searchHistoryDataStore.updateData { h ->
            SearchHistory.newBuilder()
                .addEntries(entry)
                .addAllEntries(
                    h.entriesList.filter { it.keyword != entry.keyword }.take(
                        MAX_SEARCH_HISTORY_COUNT - 1
                    )
                )
                .build()
        }
    }

    suspend fun removeSearch(entry: SearchHistory.Entry) {
        App.instance.searchHistoryDataStore.updateData { h ->
            SearchHistory.newBuilder()
                .addAllEntries(h.entriesList.filter { it.keyword != entry.keyword })
                .build()
        }
    }

    suspend fun clearSearch() {
        App.instance.searchHistoryDataStore.updateData { _ ->
            SearchHistory.getDefaultInstance()
        }
    }

    suspend fun updateHistory(entry: HistoryEntry) {
        withContext(Dispatchers.IO) {
            val dao = App.instance.db.historyDao()
            dao.addHistory(entry)
            dao.purge(MAX_HISTORY_ENTRY_COUNT)
        }
    }

    suspend fun deleteHistory(entry: HistoryEntry) {
        withContext(Dispatchers.IO) {
            val dao = App.instance.db.historyDao()
            dao.deleteHistory(entry)
        }
    }
}
