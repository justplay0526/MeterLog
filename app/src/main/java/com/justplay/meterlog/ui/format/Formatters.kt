package com.justplay.meterlog.ui.format

import com.google.firebase.Timestamp
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val numberFormatter = DecimalFormat("#,##0.##")

fun Timestamp?.formatDate(): String {
    if (this == null) return "未記錄"
    val instant = Instant.ofEpochSecond(seconds, nanoseconds.toLong())
    return dateFormatter.format(instant.atZone(ZoneId.systemDefault()))
}

fun LocalDate.formatUiDate(): String = dateFormatter.format(this)

fun Double.formatNumber(): String = numberFormatter.format(this)
