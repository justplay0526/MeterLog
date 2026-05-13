package com.justplay.meterlog.data

import com.google.firebase.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

const val MIN_READING_INTERVAL_MONTHS = 2L

fun Timestamp.toLocalDate(): LocalDate {
    return Instant.ofEpochSecond(seconds, nanoseconds.toLong())
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

fun latestReadingDate(readings: List<Reading>): LocalDate? {
    return readings
        .mapNotNull { it.recordedAt?.toLocalDate() }
        .maxOrNull()
}

fun nextAllowedReadingDate(readings: List<Reading>): LocalDate? {
    return latestReadingDate(readings)?.plusMonths(MIN_READING_INTERVAL_MONTHS)
}

fun canRecordReadingOn(readings: List<Reading>, date: LocalDate): Boolean {
    val nextAllowedDate = nextAllowedReadingDate(readings) ?: return true
    return !date.isBefore(nextAllowedDate)
}
