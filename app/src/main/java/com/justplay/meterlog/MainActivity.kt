package com.justplay.meterlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.justplay.meterlog.ui.MeterLogApp
import com.justplay.meterlog.ui.theme.MeterLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MeterLogTheme {
                MeterLogApp()
            }
        }
    }
}
