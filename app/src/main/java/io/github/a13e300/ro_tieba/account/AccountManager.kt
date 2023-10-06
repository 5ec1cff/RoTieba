package io.github.a13e300.ro_tieba.account

import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.api.TiebaClient
import io.github.a13e300.ro_tieba.api.TiebaLoginClient
import io.github.a13e300.ro_tieba.datastore.copy
import io.github.a13e300.ro_tieba.db.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class AccountManager {
    companion object {
        const val ACCOUNT_ANONYMOUS = "anonymous"
    }

    private val _currentAccount = MutableSharedFlow<Account>(replay = 1)
    val currentAccount
        get() = _currentAccount.asSharedFlow()

    suspend fun initAccount() {
        val currentUid = App.instance.settingsDataStore.data.first().currentAccount
        withContext(Dispatchers.IO) {
            val account = App.instance.db.accountDao().getAccount(currentUid) ?: Account()
            App.instance.client = TiebaClient(account)
            _currentAccount.emit(account)
        }
    }

    private suspend fun updateAccount(account: Account) {
        App.instance.client = TiebaClient(account)
        if (account.uid != ACCOUNT_ANONYMOUS)
            App.instance.db.accountDao().addAccount(account)
        App.instance.settingsDataStore.updateData { it.copy { currentAccount = account.uid } }
        _currentAccount.emit(account)
    }

    suspend fun switchAccount(uid: String) {
        withContext(Dispatchers.IO) {
            val account =
                if (uid != ACCOUNT_ANONYMOUS) {
                    val account = App.instance.db.accountDao().getAccount(uid)
                        ?: error("account $uid not exists")
                    TiebaLoginClient().login(account.bduss!!)
                } else Account()
            updateAccount(account)
        }
    }

    suspend fun addAccount(bduss: String, sToken: String? = null, baiduId: String? = null) {
        withContext(Dispatchers.IO) {
            val account = TiebaLoginClient().login(bduss, sToken, baiduId)
            updateAccount(account)
        }
    }

    suspend fun deleteAccount(account: Account) {
        withContext(Dispatchers.IO) {
            App.instance.db.accountDao().removeAccount(account)
        }
    }
}
