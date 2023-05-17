package io.github.a13e300.ro_tieba.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import io.github.a13e300.ro_tieba.account.AccountManager
import kotlinx.coroutines.flow.Flow

@Entity
data class Account(
    @PrimaryKey
    val uid: String = AccountManager.ACCOUNT_ANONYMOUS,
    val name: String? = null,
    val portrait: String? = null,
    val tbs: String? = null,
    val bduss: String? = null,
    val stoken: String? = null
)

@Dao
interface AccountDao {
    @Query("SELECT * FROM Account")
    fun getAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM Account WHERE uid = :uid")
    fun getAccount(uid: String): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAccount(account: Account)

    @Delete
    fun removeAccount(account: Account)
}
