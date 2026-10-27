package com.trainwithme.core.model.progress

import com.trainwithme.core.model.training.LoggedSet
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class ProgressStatsTest {

    private val day0 = LocalDate.of(2026, 1, 5) // a Monday

    @Test
    fun `rolling average smooths daily noise`() {
        val points = (0..6).map { WeightPoint(day0.plusDays(it.toLong()), 80.0 + it) }
        val smoothed = ProgressStats.rollingAverage(points, windowDays = 7)
        assertEquals(7, smoothed.size)
        assertEquals(80.0, smoothed.first().weightKg, 0.001)
        assertEquals(83.0, smoothed.last().weightKg, 0.01)
    }

    @Test
    fun `trend needs both weekly windows`() {
        val sparse = listOf(WeightPoint(day0, 80.0), WeightPoint(day0.plusDays(3), 79.5))
        assertNull(ProgressStats.weeklyTrendKgPerWeek(sparse))
    }

    @Test
    fun `trend compares the last two weeks`() {
        val points = buildList {
            (0..6L).forEach { add(WeightPoint(day0.plusDays(it), 80.0)) }
            (7..13L).forEach { add(WeightPoint(day0.plusDays(it), 79.0)) }
        }
        val trend = ProgressStats.weeklyTrendKgPerWeek(points)
        assertEquals(-1.0, trend!!, 0.001)
    }

    @Test
    fun `adherence is clamped between zero and one`() {
        assertEquals(0.75, ProgressStats.adherence(3, 4), 0.001)
        assertEquals(0.0, ProgressStats.adherence(0, 0), 0.001)
        assertEquals(1.0, ProgressStats.adherence(9, 4), 0.001)
    }

    @Test
    fun `streak counts back over planned days and tolerates today`() {
        val planned = (0..9L).map { day0.plusDays(it * 2) } // every other day
        val today = planned.last()
        val completed = planned.dropLast(1).toSet()
        assertEquals(9, ProgressStats.currentStreak(planned, completed, today))

        val withTodayDone = completed + today
        assertEquals(10, ProgressStats.currentStreak(planned, withTodayDone, today))
    }

    @Test
    fun `streak breaks on a missed session`() {
        val planned = (0..4L).map { day0.plusDays(it) }
        val completed = setOf(planned[0], planned[1], planned[3])
        assertEquals(1, ProgressStats.currentStreak(planned, completed, planned[3]))
    }

    @Test
    fun `personal records use estimated one rep max`() {
        val history = listOf(
            DatedSet("bench", day0, LoggedSet(0, 60.0, 10)),
            DatedSet("bench", day0.plusDays(7), LoggedSet(0, 70.0, 5)),
            DatedSet("squat", day0, LoggedSet(0, 100.0, 5)),
        )
        val records = ProgressStats.personalRecords(history)
        assertEquals(2, records.size)
        // 100x5 -> 116.7 beats 70x5 -> 81.7 for e1RM ordering
        assertEquals("squat", records.first().exerciseId)
        assertEquals(100.0, records.first().weightKg, 0.001)
    }

    @Test
    fun `warm up sets never become PRs`() {
        val history = listOf(
            DatedSet("bench", day0, LoggedSet(0, 200.0, 10, isWarmup = true)),
            DatedSet("bench", day0.plusDays(7), LoggedSet(0, 70.0, 5)),
        )
        val records = ProgressStats.personalRecords(history)
        assertEquals(70.0, records.single().weightKg, 0.001)
    }

    @Test
    fun `weekly volume groups by monday`() {
        val history = listOf(
            DatedSet("bench", day0, LoggedSet(0, 50.0, 10)),
            DatedSet("bench", day0.plusDays(2), LoggedSet(0, 50.0, 10)),
            DatedSet("bench", day0.plusDays(7), LoggedSet(0, 60.0, 10)),
        )
        val volume = ProgressStats.weeklyVolume(history)
        assertEquals(2, volume.size)
        assertEquals(1000.0, volume[0].volumeKg, 0.001)
        assertEquals(2, volume[0].workingSets)
        assertEquals(600.0, volume[1].volumeKg, 0.001)
    }

    @Test
    fun `week start is monday for every day of the week`() {
        (0..6L).forEach { offset ->
            val date = LocalDate.of(2026, 3, 2).plusDays(offset)
            val start = ProgressStats.weekStart(date)
            assertEquals(java.time.DayOfWeek.MONDAY, start.dayOfWeek)
            assertTrue(!start.isAfter(date))
        }
    }

    @Test
    fun `delta helpers report meaningful change only`() {
        assertEquals(1.5, ProgressStats.deltaKg(80.0, 81.5)!!, 0.001)
        assertTrue(ProgressStats.isWeightChangeMeaningful(80.0, 80.1).not())
        assertTrue(ProgressStats.isWeightChangeMeaningful(80.0, 80.5))
    }
}
