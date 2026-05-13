package com.justplay.meterlog.data

import com.google.firebase.Timestamp

data class Reading(
    val id: String = "",
    val value: Double = 0.0,
    val recordedAt: Timestamp? = null,
    val note: String = "",
    val createdAt: Timestamp? = null
)
