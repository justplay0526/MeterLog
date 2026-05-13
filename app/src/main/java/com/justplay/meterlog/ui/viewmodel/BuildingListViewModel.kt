package com.justplay.meterlog.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.justplay.meterlog.data.Building
import com.justplay.meterlog.data.MeterRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class BuildingListViewModel(
    uid: String,
    repository: MeterRepository = MeterRepository()
) : ViewModel() {
    val uiState: StateFlow<BuildingListUiState> = repository.observeBuildings(uid)
        .map { BuildingListUiState(buildings = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BuildingListUiState(loading = true)
        )

    class Factory(private val uid: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return BuildingListViewModel(uid) as T
        }
    }
}

data class BuildingListUiState(
    val loading: Boolean = false,
    val buildings: List<Building> = emptyList()
)
