package io.github.a13e300.ro_tieba

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.room.Room
import com.google.gson.Gson
import io.github.a13e300.ro_tieba.account.AccountManager
import io.github.a13e300.ro_tieba.api.TiebaClient
import io.github.a13e300.ro_tieba.datastore.Settings
import io.github.a13e300.ro_tieba.db.AppDataBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class App : Application() {
    companion object {
        lateinit var instance: App
            private set
        val gson = Gson()
        val appScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    }

    lateinit var db: AppDataBase
    lateinit var client: TiebaClient
    val accountManager = AccountManager()

    val settingsDataStore: DataStore<Settings> by dataStore(
        fileName = "settings.pb",
        serializer = SettingsSerializer
    )

    override fun onCreate() {
        super.onCreate()
        instance = this
        db = Room.databaseBuilder(this, AppDataBase::class.java, "app-db").build()
        appScope.launch {
            accountManager.initAccount()
        }
    }
}