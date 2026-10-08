package app.swisszen.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.swisszen.bell.BellConfig
import app.swisszen.bell.BellMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class AppSettings(
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val whLevel: String = "beg",
    val whExpertRounds: Int = 4,
)

class SettingsStore(private val context: Context) {
    private object K {
        val sound = booleanPreferencesKey("sound")
        val haptics = booleanPreferencesKey("haptics")
        val whLevel = stringPreferencesKey("wh_level")
        val whExpertRounds = intPreferencesKey("wh_expert_rounds")
        val bellOn = booleanPreferencesKey("bell_on")
        val bellMode = stringPreferencesKey("bell_mode")
        val randMin = intPreferencesKey("bell_rand_min")
        val randMax = intPreferencesKey("bell_rand_max")
        val interval = intPreferencesKey("bell_interval")
        val times = stringPreferencesKey("bell_times")
        val quietOn = booleanPreferencesKey("quiet_on")
        val quietFrom = intPreferencesKey("quiet_from")
        val quietUntil = intPreferencesKey("quiet_until")
        val nextBell = longPreferencesKey("bell_next")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            sound = p[K.sound] ?: true,
            haptics = p[K.haptics] ?: true,
            whLevel = p[K.whLevel] ?: "beg",
            whExpertRounds = p[K.whExpertRounds] ?: 4,
        )
    }

    val bell: Flow<BellConfig> = context.dataStore.data.map(::readBell)
    val nextBellAt: Flow<Long?> = context.dataStore.data.map { it[K.nextBell] }

    suspend fun bellNow(): BellConfig = bell.first()

    private fun readBell(p: Preferences) = BellConfig(
        enabled = p[K.bellOn] ?: false,
        mode = p[K.bellMode]?.let { runCatching { BellMode.valueOf(it) }.getOrNull() } ?: BellMode.RANDOM,
        randomMin = p[K.randMin] ?: 15,
        randomMax = p[K.randMax] ?: 90,
        intervalMin = p[K.interval] ?: 45,
        times = p[K.times]?.split(',')?.filter { it.isNotBlank() }?.map(String::toInt)?.sorted()
            ?: listOf(9 * 60, 13 * 60, 17 * 60 + 30),
        quietEnabled = p[K.quietOn] ?: true,
        quietFrom = p[K.quietFrom] ?: 22 * 60,
        quietUntil = p[K.quietUntil] ?: 8 * 60,
    )

    suspend fun setSound(on: Boolean) = context.dataStore.edit { it[K.sound] = on }
    suspend fun setHaptics(on: Boolean) = context.dataStore.edit { it[K.haptics] = on }
    suspend fun setWimHof(level: String, expertRounds: Int) = context.dataStore.edit {
        it[K.whLevel] = level; it[K.whExpertRounds] = expertRounds
    }

    suspend fun updateBell(transform: (BellConfig) -> BellConfig): BellConfig {
        var result: BellConfig? = null
        context.dataStore.edit { p ->
            val c = transform(readBell(p))
            p[K.bellOn] = c.enabled
            p[K.bellMode] = c.mode.name
            p[K.randMin] = c.randomMin
            p[K.randMax] = c.randomMax
            p[K.interval] = c.intervalMin
            p[K.times] = c.times.joinToString(",")
            p[K.quietOn] = c.quietEnabled
            p[K.quietFrom] = c.quietFrom
            p[K.quietUntil] = c.quietUntil
            result = c
        }
        return result!!
    }

    suspend fun setNextBell(at: Long?) = context.dataStore.edit {
        if (at == null) it.remove(K.nextBell) else it[K.nextBell] = at
    }
}
