package app.swisszen.breath

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A Wim Hof level. [inhale]/[exhale] set the tempo of one power breath in seconds,
 * [breaths] ramps inside the level's range round by round, [holds] are the per-round
 * breath-hold targets (seconds) and [ladder] their display labels.
 */
data class WimHofLevel(
    val id: String,
    val defaultRounds: Int,
    val maxRounds: Int,
    val breaths: List<Int>,
    val breathsLabel: String,
    val inhale: Double,
    val exhale: Double,
    val holds: List<Int>,
    val ladder: List<String>,
) {
    fun estimateSeconds(rounds: Int): Double =
        (0 until rounds).sumOf { r ->
            WimHofEngine.INTRO + breaths[r] * (inhale + exhale) + WimHofEngine.LAST_IN + WimHofEngine.LAST_OUT +
                holds[r] + WimHofEngine.REC_IN + WimHofEngine.REC_HOLD + WimHofEngine.REC_OUT
        }

    companion object {
        val BEGINNER = WimHofLevel("beg", 3, 3, listOf(30, 30, 30), "30", 2.0, 1.6, listOf(30, 60, 90), listOf("0:30", "1:00", "1:30"))
        val MEDIUM = WimHofLevel("med", 3, 3, listOf(30, 33, 35), "30–35", 1.6, 1.3, listOf(60, 90, 105), listOf("1:00", "1:30", "1:30–2:00"))
        val ADVANCED = WimHofLevel("adv", 4, 4, listOf(35, 37, 38, 40), "35–40", 1.4, 1.0, listOf(60, 90, 120, 150), listOf("1:00", "1:30", "2:00", "2:30"))
        val EXPERT = WimHofLevel("exp", 4, 5, listOf(40, 43, 46, 50, 50), "40–50", 1.1, 0.8, listOf(90, 120, 150, 180, 180), listOf("1:30", "2:00", "2:30", "3:00+", "3:00+"))
        val ALL = listOf(BEGINNER, MEDIUM, ADVANCED, EXPERT)
        fun byId(id: String) = ALL.firstOrNull { it.id == id } ?: BEGINNER
    }
}

enum class WhPhase { INTRO, IN, OUT, LAST_IN, LAST_OUT, HOLD, REC_IN, REC_HOLD, REC_OUT, DONE }

/**
 * Pure state machine for a guided Wim Hof session, advanced by [tick] with elapsed seconds.
 * Round: intro → N × (in, out) → last in → let go → hold (open-ended, ended by [breatheIn])
 * → recovery in → 15 s hold → let go → next round or DONE.
 */
class WimHofEngine(val level: WimHofLevel, val rounds: Int = level.defaultRounds) {
    var phase = WhPhase.INTRO; private set
    var t = 0.0; private set           // seconds into the current phase
    var total = 0.0; private set       // seconds of the whole session (paused time excluded)
    var round = 1; private set
    var breath = 1; private set
    var paused = false; private set
    val holds = mutableListOf<Double>()

    val breathsThisRound get() = level.breaths[round - 1]
    val holdTarget get() = level.holds[round - 1]
    val isPausable get() = phase in setOf(WhPhase.INTRO, WhPhase.IN, WhPhase.OUT, WhPhase.LAST_IN, WhPhase.LAST_OUT)

    fun duration(p: WhPhase = phase): Double = when (p) {
        WhPhase.INTRO -> INTRO
        WhPhase.IN -> level.inhale
        WhPhase.OUT -> level.exhale
        WhPhase.LAST_IN -> LAST_IN
        WhPhase.LAST_OUT -> LAST_OUT
        WhPhase.HOLD -> Double.POSITIVE_INFINITY
        WhPhase.REC_IN -> REC_IN
        WhPhase.REC_HOLD -> REC_HOLD
        WhPhase.REC_OUT -> REC_OUT
        WhPhase.DONE -> 0.0
    }

    /** Advances time; returns the phases entered during this tick (for sounds and haptics). */
    fun tick(dt: Double): List<WhPhase> {
        if (paused || phase == WhPhase.DONE) return emptyList()
        val entered = mutableListOf<WhPhase>()
        var left = dt
        while (left > 0 && phase != WhPhase.DONE) {
            val remaining = duration() - t
            if (left < remaining) { t += left; total += left; left = 0.0 }
            else { total += remaining; left -= remaining; advance(); entered += phase }
        }
        return entered
    }

    fun togglePause() { if (isPausable || paused) paused = !paused }

    /** Ends the empty-lungs hold; returns false if not in a hold. */
    fun breatheIn(): Boolean {
        if (phase != WhPhase.HOLD || paused) return false
        holds += t
        enter(WhPhase.REC_IN)
        return true
    }

    /** Ends the session early (End button). */
    fun finish() { enter(WhPhase.DONE) }

    private fun enter(p: WhPhase) { phase = p; t = 0.0 }

    private fun advance() {
        when (phase) {
            WhPhase.INTRO -> { breath = 1; enter(WhPhase.IN) }
            WhPhase.IN -> enter(WhPhase.OUT)
            WhPhase.OUT -> if (breath < breathsThisRound) { breath++; enter(WhPhase.IN) } else enter(WhPhase.LAST_IN)
            WhPhase.LAST_IN -> enter(WhPhase.LAST_OUT)
            WhPhase.LAST_OUT -> enter(WhPhase.HOLD)
            WhPhase.HOLD -> Unit // open-ended, only breatheIn() leaves it
            WhPhase.REC_IN -> enter(WhPhase.REC_HOLD)
            WhPhase.REC_HOLD -> enter(WhPhase.REC_OUT)
            WhPhase.REC_OUT -> if (round < rounds) { round++; enter(WhPhase.INTRO) } else enter(WhPhase.DONE)
            WhPhase.DONE -> Unit
        }
    }

    // ---- Visual helpers (orb scale 0..1 of the stage, ring progress 0..1) ----
    private fun p() = if (duration().isInfinite() || duration() == 0.0) 0.0 else (t / duration()).coerceIn(0.0, 1.0)
    private fun eInOut(x: Double) = 0.5 - 0.5 * cos(PI * x)
    private fun eOut(x: Double) = 1 - (1 - x) * (1 - x)

    fun orbScale(): Float = when (phase) {
        WhPhase.INTRO -> S_MIN + 0.03 * sin(total * 2.2)
        WhPhase.IN, WhPhase.LAST_IN -> S_MIN + (S_MAX - S_MIN) * eInOut(p())
        WhPhase.OUT, WhPhase.REC_OUT -> S_MAX - (S_MAX - S_MIN) * eOut(p())
        WhPhase.LAST_OUT -> S_MAX - (S_MAX - S_EMPTY) * eOut(p())
        WhPhase.HOLD -> S_EMPTY + 0.006 * sin(t * 1.3)
        WhPhase.REC_IN -> S_EMPTY + (S_MAX - S_EMPTY) * eInOut(p())
        WhPhase.REC_HOLD -> S_MAX + 0.008 * sin(t * 1.5)
        WhPhase.DONE -> S_MIN
    }.toFloat()

    fun ringProgress(): Float = when (phase) {
        WhPhase.IN -> (breath - 1 + p() * 0.5) / breathsThisRound
        WhPhase.OUT -> (breath - 0.5 + p() * 0.5) / breathsThisRound
        WhPhase.LAST_IN -> 1.0
        WhPhase.LAST_OUT -> 1 - p()
        WhPhase.HOLD -> (t / holdTarget).coerceAtMost(1.0)
        WhPhase.REC_HOLD -> 1 - p()
        else -> 0.0
    }.toFloat()

    companion object {
        const val INTRO = 3.0
        const val LAST_IN = 2.6
        const val LAST_OUT = 3.4
        const val REC_IN = 2.6
        const val REC_HOLD = 15.0
        const val REC_OUT = 3.0
        const val S_MIN = 0.52
        const val S_MAX = 1.0
        const val S_EMPTY = 0.4
    }
}
