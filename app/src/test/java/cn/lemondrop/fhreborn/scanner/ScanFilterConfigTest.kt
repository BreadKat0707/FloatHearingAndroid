package cn.lemondrop.fhreborn.scanner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanFilterConfigTest {

    @Test
    fun disabledFilterKeepsShortAndUnknownSongs() {
        val filter = ScanFilterConfig(durationFilterEnabled = false, durationSeconds = 30)
        assertTrue(filter.acceptsDuration(0L))
        assertTrue(filter.acceptsDuration(null))
        assertTrue(filter.acceptsDuration(1_000L))
        assertTrue(filter.acceptsDuration(30_000L))
    }

    @Test
    fun enabledFilterEnforcesMinimumDurationBoundaries() {
        val filter = ScanFilterConfig(durationFilterEnabled = true, durationSeconds = 30)
        assertFalse(filter.acceptsDuration(0L))
        assertFalse(filter.acceptsDuration(null))
        assertFalse(filter.acceptsDuration(29_999L))
        assertTrue(filter.acceptsDuration(30_000L))
        assertTrue(filter.acceptsDuration(60_000L))
        assertTrue(filter.acceptsDuration(10_000L + 60_000L))
    }

    @Test
    fun enabledFilterKeepsExactTenSecondSongAtTenSecondThreshold() {
        val filter = ScanFilterConfig(durationFilterEnabled = true, durationSeconds = 10)
        assertFalse(filter.acceptsDuration(9_999L))
        assertTrue(filter.acceptsDuration(10_000L))
    }
}
