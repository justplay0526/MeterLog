package com.justplay.meterlog.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.justplay.meterlog.data.MeterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BuildingFormViewModel(
    private val uid: String,
    private val repository: MeterRepository = MeterRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(BuildingFormUiState())
    val uiState = _uiState.asStateFlow()

    fun updateName(value: String) {
        _uiState.value = _uiState.value.copy(name = value)
    }

    fun updateFloorCount(value: String) {
        val state = _uiState.value
        val floorCount = value.toIntOrNull()?.coerceIn(1, 99) ?: 1
        _uiState.value = state.copy(
            floorCountText = value,
            waterCountsByFloor = buildCountsForFloors(
                floorCount = floorCount,
                currentCounts = state.waterCountsByFloor,
                manualFloors = state.manualWaterFloors,
                defaultCount = state.defaultWaterCount
            ),
            electricCountsByFloor = buildCountsForFloors(
                floorCount = floorCount,
                currentCounts = state.electricCountsByFloor,
                manualFloors = state.manualElectricFloors,
                defaultCount = state.defaultElectricCount
            ),
            manualWaterFloors = state.manualWaterFloors.filter { it <= floorCount }.toSet(),
            manualElectricFloors = state.manualElectricFloors.filter { it <= floorCount }.toSet()
        )
    }

    fun updateDefaultWaterCount(value: String) {
        _uiState.value = _uiState.value.copy(defaultWaterCount = value)
    }

    fun updateDefaultElectricCount(value: String) {
        _uiState.value = _uiState.value.copy(defaultElectricCount = value)
    }

    fun applyDefaultsToAllFloors() {
        val state = _uiState.value
        val floorCount = state.floorCountText.toIntOrNull()?.coerceIn(1, 99) ?: 1
        _uiState.value = state.copy(
            waterCountsByFloor = buildCountsForFloors(
                floorCount = floorCount,
                currentCounts = state.waterCountsByFloor,
                manualFloors = state.manualWaterFloors,
                defaultCount = state.defaultWaterCount
            ),
            electricCountsByFloor = buildCountsForFloors(
                floorCount = floorCount,
                currentCounts = state.electricCountsByFloor,
                manualFloors = state.manualElectricFloors,
                defaultCount = state.defaultElectricCount
            )
        )
    }

    fun updateWaterCount(floor: Int, value: String) {
        val state = _uiState.value
        _uiState.value = state.copy(
            waterCountsByFloor = state.waterCountsByFloor + (floor to value),
            manualWaterFloors = state.manualWaterFloors + floor
        )
    }

    fun updateElectricCount(floor: Int, value: String) {
        val state = _uiState.value
        _uiState.value = state.copy(
            electricCountsByFloor = state.electricCountsByFloor + (floor to value),
            manualElectricFloors = state.manualElectricFloors + floor
        )
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        val floorCount = state.floorCountText.toIntOrNull()
        if (state.name.isBlank()) {
            _uiState.value = state.copy(error = "建物名稱不可空白")
            return
        }
        if (floorCount == null || floorCount <= 0) {
            _uiState.value = state.copy(error = "樓層數必須大於 0")
            return
        }

        val waterCounts = (1..floorCount).associateWith { floor ->
            state.waterCountsByFloor[floor].asNonNegativeInt()
        }
        val electricCounts = (1..floorCount).associateWith { floor ->
            state.electricCountsByFloor[floor].asNonNegativeInt()
        }
        if (waterCounts.values.sum() + electricCounts.values.sum() == 0) {
            _uiState.value = state.copy(error = "至少需要建立一顆水表或電表")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(loading = true, error = null)
            runCatching {
                repository.createBuildingWithMeters(
                    uid = uid,
                    name = state.name,
                    floorCount = floorCount,
                    waterCountsByFloor = waterCounts,
                    electricCountsByFloor = electricCounts
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(loading = false)
                onSaved()
            }.onFailure {
                _uiState.value = _uiState.value.copy(loading = false, error = it.message ?: "儲存失敗")
            }
        }
    }

    private fun buildCountsForFloors(
        floorCount: Int,
        currentCounts: Map<Int, String>,
        manualFloors: Set<Int>,
        defaultCount: String
    ): Map<Int, String> {
        return (1..floorCount).associateWith { floor ->
            if (floor in manualFloors) {
                currentCounts[floor].orEmpty()
            } else {
                defaultCount
            }
        }
    }

    private fun String?.asNonNegativeInt(): Int {
        return this?.toIntOrNull()?.coerceAtLeast(0) ?: 0
    }

    class Factory(private val uid: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return BuildingFormViewModel(uid) as T
        }
    }
}

data class BuildingFormUiState(
    val loading: Boolean = false,
    val name: String = "",
    val floorCountText: String = "1",
    val defaultWaterCount: String = "0",
    val defaultElectricCount: String = "0",
    val waterCountsByFloor: Map<Int, String> = mapOf(1 to "0"),
    val electricCountsByFloor: Map<Int, String> = mapOf(1 to "0"),
    val manualWaterFloors: Set<Int> = emptySet(),
    val manualElectricFloors: Set<Int> = emptySet(),
    val error: String? = null
)
