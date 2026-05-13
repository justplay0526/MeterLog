package com.justplay.meterlog.ui.state

data class UiState<T>(
    val loading: Boolean = false,
    val data: T,
    val error: String? = null
)
