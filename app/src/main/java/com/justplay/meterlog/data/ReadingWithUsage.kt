package com.justplay.meterlog.data

data class ReadingWithUsage(
    val reading: Reading,
    val usageFromPrevious: Double?,
    val isAnomaly: Boolean
)
