package io.github.a13e300.ro_tieba

import android.app.Application
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.room.Room
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.SketchFactory
import com.github.panpf.sketch.decode.GifAnimatedDrawableDecoder
import com.github.panpf.sketch.decode.GifMovieDrawableDecoder
import com.github.panpf.sketch.http.OkHttpStack
import com.google.gson.Gson
import io.github.a13e300.ro_tieba.account.AccountManager
import io.github.a13e300.ro_tieba.api.TiebaClient
import io.github.a13e300.ro_tieba.datastore.Settings
import io.github.a13e300.ro_tieba.db.AppDataBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class App : Application(), SketchFactory {
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

    override fun createSketch(): Sketch {
        return Sketch.Builder(this).apply {
            components {
                addDrawableDecoder(
                    when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> GifAnimatedDrawableDecoder.Factory()
                        else -> GifMovieDrawableDecoder.Factory()
                    }
                )
            }
            httpStack(OkHttpStack.Builder().build())
        }.build()
    }
}