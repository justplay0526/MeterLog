package com.justplay.meterlog.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.justplay.meterlog.data.Building
import com.justplay.meterlog.data.Meter
import com.justplay.meterlog.data.MeterRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class BuildingDetailViewModel(
    uid: String,
    buildingId: String,
    repository: MeterRepository = MeterRepository()
) : ViewModel() {
    val uiState: StateFlow<BuildingDetailUiState> = combine(
        repository.observeBuilding(uid, buildingId),
        repository.observeBuildingMeters(uid, buildingId)
    ) { building, meters ->
        BuildingDetailUiState(
            building = building,
            metersByFloor = meters.groupBy { it.floorNumber }.toSortedMap()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BuildingDetailUiState(loading = true)
    )

    class Factory(private val uid: String, private val buildingId: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return BuildingDetailViewModel(uid, buildingId) as T
        }
    }
}

data class BuildingDetailUiState(
    val loading: Boolean = false,
    val building: Building? = null,
    val metersByFloor: Map<Int, List<Meter>> = emptyMap()
)
