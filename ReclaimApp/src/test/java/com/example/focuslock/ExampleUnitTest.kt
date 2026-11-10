package com.example.focuslock

import org.junit.Test
import org.junit.Assert.*
import java.util.Calendar

class ExampleUnitTest {
    @Test
    fun testMorningHoursCheck() {
        fun isMorning(hour: Int): Boolean = hour in Config.MORNING_START_HOUR until Config.MORNING_END_HOUR

        assertFalse("4:00 AM should not be morning hours", isMorning(4))
        assertTrue("5:00 AM should be morning hours", isMorning(5))
        assertTrue("7:30 AM should be morning hours", isMorning(7))
        assertTrue("8:59 AM should be morning hours", isMorning(8))
        assertFalse("9:00 AM should not be morning hours", isMorning(9))
        assertFalse("12:00 PM should not be morning hours", isMorning(12))
        assertFalse("11:00 PM should not be morning hours", isMorning(23))
    }

    @Test
    fun testMsUntilMorningEnds() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val remainingMs = Config.getMsUntilMorningEnds(cal)
        val expectedMs = 2 * 60 * 60 * 1000L // 2 hours from 7:00 AM to 9:00 AM
        assertEquals(expectedMs, remainingMs)
    }
}