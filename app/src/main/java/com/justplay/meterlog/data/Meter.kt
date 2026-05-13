package com.justplay.meterlog.data

import com.google.firebase.Timestamp

data class Meter(
    val id: String = "",
    val buildingId: String = "",
    val name: String = "",
    val type: MeterType = MeterType.WATER,
    val floorNumber: Int = 1,
    val locationLabel: String = "",
    val note: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
