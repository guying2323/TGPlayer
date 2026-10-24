package com.tgplayer.android.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FormatTest {

    @Test
    fun durationIsZeroPaddedBelowAnHour() {
        assertEquals("00:00", formatDuration(0))
        assertEquals("00:59", formatDuration(59))
        assertEquals("01:30", formatDuration(90))
        assertEquals("59:59", formatDuration(3599))
    }

    @Test
    fun durationShowsHoursWhenLong() {
        assertEquals("1:00:00", formatDuration(3600))
        assertEquals("1:01:01", formatDuration(3661))
        assertEquals("10:00:00", formatDuration(36_000))
    }

    @Test
    fun negativeDurationIsClamped() {
        assertEquals("00:00", formatDuration(-5))
    }

    @Test
    fun sizeIsEmptyForUnknown() {
        assertEquals("", formatSize(0))
        assertEquals("", formatSize(-1))
    }

    @Test
    fun sizeUsesReadableUnits() {
        assertEquals("512 KB", formatSize(512L * 1024))
        assertEquals("5 MB", formatSize(5L * 1024 * 1024))
        assertEquals("2.5 GB", formatSize((2.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun resolutionBuckets() {
        assertEquals("4K", resolutionLabel(3840, 2160))
        assertEquals("1440p", resolutionLabel(2560, 1440))
        assertEquals("1080p", resolutionLabel(1920, 1080))
        assertEquals("720p", resolutionLabel(1280, 720))
        assertEquals("360p", resolutionLabel(640, 360))
        assertEquals("", resolutionLabel(0, 0))
    }

    @Test
    fun dateIsEmptyForZero() {
        assertEquals("", formatDateShort(0))
        assertEquals("", formatDateShort(-10))
    }

    @Test
    fun dateFormatsYearMonthDay() {
        val expected = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(86_400_000L))
        assertEquals(expected, formatDateShort(86_400))
    }
}
