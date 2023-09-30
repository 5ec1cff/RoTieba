package io.github.a13e300.ro_tieba.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

enum class EntryType {
    FORUM,
    THREAD,
    USER
}

@Entity(primaryKeys = ["type", "id"])
data class HistoryEntry(
    val type: EntryType,
    val id: String, // forum, thread, user
    val time: Long,
    val title: String = "",
    val forumName: String = "",
    val forumAvatar: String = "",
    val userAvatar: String = "",
    val userName: String = "",
    val userNick: String = "",
    val userId: Long = 0,
    val postId: Long = 0,
    val floor: Int = 0,
)

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addHistory(entry: HistoryEntry)

    @Query("DELETE FROM HistoryEntry")
    suspend fun removeAll()

    @Query("SELECT * FROM HistoryEntry ORDER BY time DESC")
    fun getHistories(): PagingSource<Int, HistoryEntry>
}
