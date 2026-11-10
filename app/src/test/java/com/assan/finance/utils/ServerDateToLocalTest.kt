package com.assan.finance.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/** Ledger dates from the server are UTC; the app must show the Pakistan date (the one the portal shows). */
class ServerDateToLocalTest {

    @Test
    fun utcMidnightPakistanBecomesNextDay() {
        // values from Ledger/recovery-schedule for borrower 0035-148001; portal shows 11-JUL-26 and 03-JUL-26
        assertEquals("2026-07-11", ServerDates.toLocalDate("2026-07-10T19:00:00.000Z"))
        assertEquals("2026-07-03", ServerDates.toLocalDate("2026-07-02T19:00:00.000Z"))
        assertEquals("2026-09-01", ServerDates.toLocalDate("2026-08-31T19:00:00.000Z"))
    }

    @Test
    fun valuesWithoutTimeZoneStayTheSame() {
        assertEquals("1990-01-01", ServerDates.toLocalDate("1990-01-01T00:00:00"))
        assertEquals("2026-07-11", ServerDates.toLocalDate("2026-07-11"))
        assertEquals("", ServerDates.toLocalDate(null))
    }

    @Test
    fun displayFormatMatchesLedger() {
        assertEquals("11-Jul-26", ServerDates.toDisplayDate("2026-07-10T19:00:00.000Z"))
        assertEquals("03-Jul-26", ServerDates.toDisplayDate("2026-07-02T19:00:00.000Z"))
        assertEquals("", ServerDates.toDisplayDate(null))
    }

    @Test
    fun offsetIsRespected() {
        assertEquals("2026-07-11", ServerDates.toLocalDate("2026-07-11T00:00:00+05:00"))
    }
}
