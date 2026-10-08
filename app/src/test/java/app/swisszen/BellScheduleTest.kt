package app.swisszen

import app.swisszen.bell.BellConfig
import app.swisszen.bell.BellMode
import app.swisszen.bell.BellSchedule
import app.swisszen.ui.streak
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

class BellScheduleTest {
    private val noon = LocalDateTime.of(2026, 10, 8, 12, 0)

    @Test fun disabledNeverRings() {
        assertNull(BellSchedule.next(noon, BellConfig(enabled = false)))
    }

    @Test fun randomStaysInsideTheRange() {
        val c = BellConfig(enabled = true, mode = BellMode.RANDOM, randomMin = 15, randomMax = 90, quietEnabled = false)
        val rnd = Random(1)
        repeat(500) {
            val gap = Duration.between(noon, BellSchedule.next(noon, c, rnd)).toMinutes()
            assertTrue("gap $gap", gap in 15..90)
        }
    }

    @Test fun intervalAddsTheInterval() {
        val c = BellConfig(enabled = true, mode = BellMode.INTERVAL, intervalMin = 45)
        assertEquals(noon.plusMinutes(45), BellSchedule.next(noon, c))
    }

    @Test fun quietHoursWrapAroundMidnight() {
        val c = BellConfig(quietEnabled = true, quietFrom = 22 * 60, quietUntil = 8 * 60)
        assertTrue(BellSchedule.inQuiet(c, 23 * 60))
        assertTrue(BellSchedule.inQuiet(c, 2 * 60))
        assertFalse(BellSchedule.inQuiet(c, 8 * 60))
        assertFalse(BellSchedule.inQuiet(c, 21 * 60 + 59))
    }

    @Test fun intervalLandingInQuietHoursMovesToMorning() {
        val c = BellConfig(enabled = true, mode = BellMode.INTERVAL, intervalMin = 60, quietFrom = 22 * 60, quietUntil = 8 * 60)
        val late = LocalDateTime.of(2026, 10, 8, 21, 30)
        assertEquals(LocalDateTime.of(2026, 10, 9, 8, 0), BellSchedule.next(late, c))
    }

    @Test fun fixedTimesPickTheNextOneAndSkipQuiet() {
        val c = BellConfig(enabled = true, mode = BellMode.TIMES, times = listOf(7 * 60, 9 * 60, 13 * 60, 23 * 60),
            quietFrom = 22 * 60, quietUntil = 8 * 60)
        assertEquals(LocalDateTime.of(2026, 10, 8, 13, 0), BellSchedule.next(noon, c))
        val evening = LocalDateTime.of(2026, 10, 8, 14, 0)
        assertEquals(LocalDateTime.of(2026, 10, 9, 9, 0), BellSchedule.next(evening, c)) // 23:00 and 07:00 are quiet
    }

    @Test fun fixedTimesEmptyMeansNoBell() {
        assertNull(BellSchedule.next(noon, BellConfig(enabled = true, mode = BellMode.TIMES, times = emptyList())))
    }

    @Test fun streakCountsConsecutiveDays() {
        val today = LocalDate.of(2026, 10, 8)
        assertEquals(0, streak(emptySet(), today))
        assertEquals(3, streak(setOf(today, today.minusDays(1), today.minusDays(2), today.minusDays(4)), today))
        // today not done yet: the streak up to yesterday still counts
        assertEquals(2, streak(setOf(today.minusDays(1), today.minusDays(2)), today))
    }
}
