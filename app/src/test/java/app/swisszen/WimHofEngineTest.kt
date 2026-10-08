package app.swisszen

import app.swisszen.breath.WhPhase
import app.swisszen.breath.WimHofEngine
import app.swisszen.breath.WimHofLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WimHofEngineTest {

    /** Runs the engine with small steps until it reaches [target], ending each hold after [holdSec]. */
    private fun runUntil(e: WimHofEngine, target: WhPhase, holdSec: Double = 0.0, maxSec: Double = 3600.0): Double {
        var t = 0.0
        while (e.phase != target && t < maxSec) {
            if (e.phase == WhPhase.HOLD && e.t >= holdSec) { e.breatheIn(); continue }
            e.tick(0.01); t += 0.01
        }
        return t
    }

    @Test fun levelsMatchTheAgreedTable() {
        with(WimHofLevel.BEGINNER) { assertEquals(3, defaultRounds); assertEquals(listOf(30, 30, 30), breaths); assertEquals(listOf(30, 60, 90), holds) }
        with(WimHofLevel.MEDIUM) { assertEquals(3, defaultRounds); assertTrue(breaths.all { it in 30..35 }); assertEquals("1:30–2:00", ladder[2]) }
        with(WimHofLevel.ADVANCED) { assertEquals(4, defaultRounds); assertTrue(breaths.all { it in 35..40 }); assertEquals(listOf(60, 90, 120, 150), holds) }
        with(WimHofLevel.EXPERT) { assertEquals(4, defaultRounds); assertEquals(5, maxRounds); assertTrue(breaths.all { it in 40..50 }); assertEquals("3:00+", ladder[3]) }
        // faster levels breathe faster
        val tempos = WimHofLevel.ALL.map { it.inhale + it.exhale }
        assertEquals(tempos.sortedDescending(), tempos)
    }

    @Test fun powerBreathingTakesBreathsTimesTempo() {
        val level = WimHofLevel.BEGINNER
        val e = WimHofEngine(level)
        runUntil(e, WhPhase.IN)
        val t = runUntil(e, WhPhase.LAST_IN)
        assertEquals(30 * (level.inhale + level.exhale), t, 0.1)
        assertEquals(30, e.breath)
    }

    @Test fun fullSessionRecordsOneHoldPerRound() {
        val e = WimHofEngine(WimHofLevel.MEDIUM)
        runUntil(e, WhPhase.DONE, holdSec = 5.0)
        assertEquals(3, e.holds.size)
        assertEquals(3, e.round)
        e.holds.forEach { assertEquals(5.0, it, 0.05) }
    }

    @Test fun holdIsOpenEndedUntilBreatheIn() {
        val e = WimHofEngine(WimHofLevel.BEGINNER)
        runUntil(e, WhPhase.HOLD)
        e.tick(500.0)
        assertEquals(WhPhase.HOLD, e.phase)
        assertTrue(e.breatheIn())
        assertEquals(WhPhase.REC_IN, e.phase)
        assertFalse(e.breatheIn())
    }

    @Test fun recoveryHoldLastsFifteenSeconds() {
        val e = WimHofEngine(WimHofLevel.BEGINNER)
        runUntil(e, WhPhase.REC_HOLD, holdSec = 1.0)
        val t = runUntil(e, WhPhase.REC_OUT)
        assertEquals(15.0, t, 0.05)
    }

    @Test fun pauseStopsTime() {
        val e = WimHofEngine(WimHofLevel.BEGINNER)
        e.tick(1.0)
        e.togglePause()
        e.tick(100.0)
        assertEquals(1.0, e.total, 1e-9)
        e.togglePause()
        e.tick(1.0)
        assertEquals(2.0, e.total, 1e-9)
    }

    @Test fun expertCanRunFiveRounds() {
        val e = WimHofEngine(WimHofLevel.EXPERT, rounds = 5)
        runUntil(e, WhPhase.DONE, holdSec = 1.0, maxSec = 10_000.0)
        assertEquals(5, e.holds.size)
    }

    @Test fun orbStaysWithinBounds() {
        val e = WimHofEngine(WimHofLevel.ADVANCED)
        var t = 0.0
        while (e.phase != WhPhase.DONE && t < 2000) {
            if (e.phase == WhPhase.HOLD && e.t > 2) e.breatheIn()
            e.tick(0.05); t += 0.05
            assertTrue(e.orbScale() in 0.35f..1.02f)
            assertTrue(e.ringProgress() in 0f..1f)
        }
    }
}
