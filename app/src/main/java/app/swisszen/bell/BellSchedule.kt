package app.swisszen.bell

import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.random.Random

enum class BellMode { RANDOM, INTERVAL, TIMES }

/** Times of day are stored as minutes since midnight. */
data class BellConfig(
    val enabled: Boolean = false,
    val mode: BellMode = BellMode.RANDOM,
    val randomMin: Int = 15,
    val randomMax: Int = 90,
    val intervalMin: Int = 45,
    val times: List<Int> = listOf(9 * 60, 13 * 60, 17 * 60 + 30),
    val quietEnabled: Boolean = true,
    val quietFrom: Int = 22 * 60,
    val quietUntil: Int = 8 * 60,
)

object BellSchedule {

    fun inQuiet(c: BellConfig, minuteOfDay: Int): Boolean {
        if (!c.quietEnabled || c.quietFrom == c.quietUntil) return false
        return if (c.quietFrom < c.quietUntil) minuteOfDay in c.quietFrom until c.quietUntil
        else minuteOfDay >= c.quietFrom || minuteOfDay < c.quietUntil
    }

    private fun LocalDateTime.minuteOfDay() = hour * 60 + minute
    private fun Int.toTime(): LocalTime = LocalTime.of(this / 60, this % 60)

    /** When the bell should ring next after [now], or null if it never will with this config. */
    fun next(now: LocalDateTime, c: BellConfig, random: Random = Random.Default): LocalDateTime? {
        if (!c.enabled) return null
        return when (c.mode) {
            BellMode.TIMES -> nextFixedTime(now, c)
            BellMode.INTERVAL -> outsideQuiet(now.plusMinutes(c.intervalMin.toLong()), c, 0)
            BellMode.RANDOM -> {
                val lo = minOf(c.randomMin, c.randomMax)
                val hi = maxOf(c.randomMin, c.randomMax)
                val gap = random.nextInt(lo, hi + 1)
                outsideQuiet(now.plusMinutes(gap.toLong()), c, random.nextInt(0, lo + 1))
            }
        }
    }

    private fun nextFixedTime(now: LocalDateTime, c: BellConfig): LocalDateTime? {
        val times = c.times.distinct().sorted().filterNot { inQuiet(c, it) }
        if (times.isEmpty()) return null
        for (day in 0..1) {
            val date = now.toLocalDate().plusDays(day.toLong())
            times.map { LocalDateTime.of(date, it.toTime()) }.firstOrNull { it.isAfter(now) }?.let { return it }
        }
        return null
    }

    /** Pushes a candidate that lands in quiet hours to the end of that window (+ a little jitter). */
    private fun outsideQuiet(candidate: LocalDateTime, c: BellConfig, jitterMin: Int): LocalDateTime {
        if (!inQuiet(c, candidate.minuteOfDay())) return candidate
        var end = LocalDateTime.of(candidate.toLocalDate(), c.quietUntil.toTime())
        if (!end.isAfter(candidate)) end = end.plusDays(1)
        return end.plusMinutes(jitterMin.toLong())
    }
}
