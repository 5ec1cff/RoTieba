package io.github.a13e300.ro_tieba.account

import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.Logger
import io.github.a13e300.ro_tieba.api.TiebaClient
import io.github.a13e300.ro_tieba.datastore.copy
import io.github.a13e300.ro_tieba.db.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class AccountManager {
    companion object {
        const val ACCOUNT_ANONYMOUS = "anonymous"
    }

    private val _currentAccount = MutableStateFlow(Account())
    val currentAccount
        get() = _currentAccount.asStateFlow()

    suspend fun initAccount() {
        val currentUid = App.instance.settingsDataStore.data.first().currentAccount
        switchAccount(currentUid)
    }

    private suspend fun switchAccountInternal(account: Account) {
        if (account.uid != ACCOUNT_ANONYMOUS)
            App.instance.db.accountDao().addAccount(account)
        App.instance.settingsDataStore.updateData { it.copy { currentAccount = account.uid } }
        _currentAccount.value = account
    }

    suspend fun switchAccount(uid: String) {
        Logger.d("switch to $uid")
        val client =
            if (uid != ACCOUNT_ANONYMOUS) {
                withContext(Dispatchers.IO) {
                    val account = App.instance.db.accountDao().getAccount(uid)
                        ?: error("account $uid not exists")
                    TiebaClient(account).apply { login(account.bduss!!) }
                }
            } else TiebaClient()
        App.instance.client = client
        withContext(Dispatchers.IO) {
            switchAccountInternal(client.getAccount())
        }
        Logger.d("switch to $uid done")
    }

    suspend fun addAccount(bduss: String) {
        Logger.d("add account")
        val client = TiebaClient()
        withContext(Dispatchers.IO) {
            val account = client.login(bduss)
            switchAccountInternal(account)
        }
        App.instance.client = client
        Logger.d("add account done")
    }

    suspend fun deleteAccount(account: Account) {
        withContext(Dispatchers.IO) {
            App.instance.db.accountDao().removeAccount(account)
        }
    }
}
