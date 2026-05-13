package com.justplay.meterlog.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.justplay.meterlog.data.Meter
import com.justplay.meterlog.data.MeterRepository
import com.justplay.meterlog.data.MeterType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class MeterListViewModel(
    private val uid: String,
    repository: MeterRepository = MeterRepository()
) : ViewModel() {
    private val selectedType = MutableStateFlow<MeterType?>(null)

    val uiState: StateFlow<MeterListUiState> = combine(
        repository.observeMeters(uid),
        selectedType
    ) { meters, type ->
        val visibleMeters = type?.let { filter -> meters.filter { it.type == filter } } ?: meters
        MeterListUiState(meters = visibleMeters, selectedType = type)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MeterListUiState(loading = true)
    )

    fun selectType(type: MeterType?) {
        selectedType.value = type
    }

    class Factory(private val uid: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return MeterListViewModel(uid) as T
        }
    }
}

data class MeterListUiState(
    val loading: Boolean = false,
    val meters: List<Meter> = emptyList(),
    val selectedType: MeterType? = null
)
