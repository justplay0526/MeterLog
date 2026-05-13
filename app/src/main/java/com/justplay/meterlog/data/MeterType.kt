package com.justplay.meterlog.data

enum class MeterType(val label: String) {
    WATER("水表"),
    ELECTRIC("電表");

    companion object {
        fun fromName(value: String?): MeterType {
            return entries.firstOrNull { it.name == value } ?: WATER
        }
    }
}
