package app.swisszen

import android.app.Application
import android.content.Context
import app.swisszen.audio.SoundSynth
import app.swisszen.bell.BellAlarms
import app.swisszen.data.SettingsStore
import app.swisszen.data.ZenDatabase
import app.swisszen.ui.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
    val app: Context = context.applicationContext
    val db = ZenDatabase.create(context)
    val settings = SettingsStore(context)
    val sound by lazy { SoundSynth(context) }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

class SwissZenApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        ThemeMode.restore(this)
        container = AppContainer(this)
        BellAlarms.ensureChannel(this)
    }
}

val Context.container: AppContainer get() = (applicationContext as SwissZenApp).container
