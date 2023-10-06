package io.github.a13e300.ro_tieba

import android.app.Application
import android.content.Context
import android.os.Build
import android.webkit.WebView
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.room.Room
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.SketchFactory
import com.github.panpf.sketch.decode.GifAnimatedDrawableDecoder
import com.github.panpf.sketch.decode.GifMovieDrawableDecoder
import com.github.panpf.sketch.http.OkHttpStack
import com.github.panpf.sketch.request.PauseLoadWhenScrollingDrawableDecodeInterceptor
import com.google.gson.Gson
import io.github.a13e300.ro_tieba.account.AccountManager
import io.github.a13e300.ro_tieba.api.TiebaClient
import io.github.a13e300.ro_tieba.datastore.SearchHistory
import io.github.a13e300.ro_tieba.datastore.Settings
import io.github.a13e300.ro_tieba.db.AppDataBase
import io.github.a13e300.ro_tieba.history.HistoryManager
import io.github.a13e300.ro_tieba.serializer.SearchHistorySerializer
import io.github.a13e300.ro_tieba.serializer.SettingsSerializer
import io.github.a13e300.ro_tieba.utils.ignoreAllSSLErrorsIfDebug
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

class App : Application(), SketchFactory {
    companion object {
        lateinit var instance: App
            private set
        val gson = Gson()
        val settings: Settings
            get() = runBlocking { instance.settingsDataStore.data.first() }
    }

    lateinit var db: AppDataBase
    lateinit var client: TiebaClient
    val accountManager = AccountManager()
    lateinit var historyManager: HistoryManager

    val settingsDataStore: DataStore<Settings> by dataStore(
        fileName = "settings.pb",
        serializer = SettingsSerializer
    )

    val searchHistoryDataStore: DataStore<SearchHistory> by dataStore(
        fileName = "search_history.pb",
        serializer = SearchHistorySerializer
    )

    override fun onCreate() {
        super.onCreate()
        instance = this
        db = Room.databaseBuilder(this, AppDataBase::class.java, "app-db").build()
        historyManager = HistoryManager()
        runBlocking {
            accountManager.initAccount()
        }
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
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
                addDrawableDecodeInterceptor(PauseLoadWhenScrollingDrawableDecodeInterceptor())
            }
            httpStack(OkHttpStack(OkHttpClient.Builder().ignoreAllSSLErrorsIfDebug().build()))
        }.build()
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }
}