package com.justplay.meterlog.data

data class MeterReadings(
    val meter: Meter,
    val readings: List<Reading> = emptyList()
) {
    val latestReading = readings.maxWithOrNull(
        compareBy<Reading> { it.recordedAt?.seconds ?: Long.MIN_VALUE }
            .thenBy { it.recordedAt?.nanoseconds ?: Int.MIN_VALUE }
    )
    val latestReadingDate = latestReadingDate(readings)
    val nextAllowedReadingDate = nextAllowedReadingDate(readings)
}
