package app.swisszen.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * Light/dark switching via AppCompat night mode, so the window background and system bars follow too.
 * AppCompat doesn't persist it, so the choice lives in plain prefs and is re-applied on app start.
 */
object ThemeMode {
    const val SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    const val LIGHT = AppCompatDelegate.MODE_NIGHT_NO
    const val DARK = AppCompatDelegate.MODE_NIGHT_YES

    private const val PREFS = "theme"
    private const val KEY = "night_mode"

    fun current(): Int = AppCompatDelegate.getDefaultNightMode().let { if (it == LIGHT || it == DARK) it else SYSTEM }

    /** Call once from Application.onCreate, before any activity is created. */
    fun restore(context: Context) {
        AppCompatDelegate.setDefaultNightMode(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY, SYSTEM))
    }

    fun set(context: Context, mode: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY, mode).apply()
        AppCompatDelegate.setDefaultNightMode(mode) // recreates the activity in the new mode
    }
}
