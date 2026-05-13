package com.justplay.meterlog.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.justplay.meterlog.data.MeterType

fun meterIcon(type: MeterType): ImageVector {
    return when (type) {
        MeterType.WATER -> Icons.Rounded.WaterDrop
        MeterType.ELECTRIC -> Icons.Rounded.Bolt
    }
}

fun meterColor(type: MeterType): Color {
    return when (type) {
        MeterType.WATER -> Color(0xFF0284C7)
        MeterType.ELECTRIC -> Color(0xFFD97706)
    }
}

@Composable
fun MeterIcon(type: MeterType, modifier: Modifier = Modifier) {
    Icon(
        imageVector = meterIcon(type),
        contentDescription = type.label,
        modifier = modifier,
        tint = meterColor(type)
    )
}
