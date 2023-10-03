package io.github.a13e300.ro_tieba.history

import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.db.HistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HistoryManager {
    companion object {
        const val MAX_HISTORY_ENTRY_COUNT = 300
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
