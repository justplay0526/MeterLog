package com.justplay.meterlog.data

import com.google.firebase.Timestamp

data class Building(
    val id: String = "",
    val name: String = "",
    val floorCount: Int = 1,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
