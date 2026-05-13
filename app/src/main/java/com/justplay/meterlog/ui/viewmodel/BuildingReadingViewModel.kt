package com.justplay.meterlog.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.justplay.meterlog.data.Meter
import com.justplay.meterlog.data.MeterRepository
import com.justplay.meterlog.data.MeterReadings
import com.justplay.meterlog.data.MeterType
import com.justplay.meterlog.data.Reading
import com.justplay.meterlog.data.canRecordReadingOn
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class BuildingReadingViewModel(
    private val uid: String,
    private val buildingId: String,
    private val meterType: MeterType,
    private val repository: MeterRepository = MeterRepository()
) : ViewModel() {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val _uiState = MutableStateFlow(
        BuildingReadingUiState(recordedDate = LocalDate.now().format(formatter))
    )
    val uiState = _uiState.asStateFlow()

    private val readingsByMeterId = mutableMapOf<String, List<Reading>>()
    private val readingJobs = mutableMapOf<String, Job>()

    init {
        viewModelScope.launch {
            repository.observeBuildingMeters(uid, buildingId).collect { meters ->
                val filteredMeters = meters.filter { it.type == meterType }
                val meterIds = filteredMeters.map { it.id }.toSet()
                readingJobs.keys.filter { it !in meterIds }.forEach { meterId ->
                    readingJobs.remove(meterId)?.cancel()
                    readingsByMeterId.remove(meterId)
                }
                filteredMeters.forEach { meter ->
                    if (readingJobs[meter.id] == null) {
                        readingJobs[meter.id] = viewModelScope.launch {
                            repository.observeBuildingReadings(uid, buildingId, meter.id).collect { readings ->
                                readingsByMeterId[meter.id] = readings
                                updateMeterRows(filteredMeters)
                            }
                        }
                    }
                }
                updateMeterRows(filteredMeters)
            }
        }
    }

    fun updateRecordedDate(value: String) {
        _uiState.value = _uiState.value.copy(recordedDate = value)
        refreshAllowedState()
    }

    fun updateValue(meterId: String, value: String) {
        _uiState.value = _uiState.value.copy(
            valuesByMeterId = _uiState.value.valuesByMeterId + (meterId to value)
        )
    }

    fun updateNote(value: String) {
        _uiState.value = _uiState.value.copy(note = value)
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        val date = runCatching { LocalDate.parse(state.recordedDate, formatter) }.getOrNull()
        if (date == null) {
            _uiState.value = state.copy(error = "日期格式請使用 YYYY-MM-DD")
            return
        }

        val rowsById = state.rows.associateBy { it.meter.id }
        val entries = mutableListOf<Pair<String, Double>>()
        state.valuesByMeterId.forEach { (meterId, rawValue) ->
            if (rawValue.isBlank()) return@forEach
            val row = rowsById[meterId]
            val value = rawValue.toDoubleOrNull()
            if (value == null) {
                _uiState.value = state.copy(error = "${row?.meter?.name ?: "表具"} 的讀數不是有效數字")
                return
            }
            if (row != null && !canRecordReadingOn(row.readings, date)) {
                _uiState.value = state.copy(error = "${row.meter.name} 尚未到下次可抄表日期")
                return
            }
            entries += meterId to value
        }

        if (entries.isEmpty()) {
            _uiState.value = state.copy(error = "請至少輸入一筆${meterType.label}讀數")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(loading = true, error = null)
            runCatching {
                val instant = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
                val timestamp = Timestamp(instant.epochSecond, 0)
                entries.forEach { (meterId, value) ->
                    repository.addBuildingReading(
                        uid = uid,
                        buildingId = buildingId,
                        meterId = meterId,
                        value = value,
                        recordedAt = timestamp,
                        note = state.note
                    )
                }
            }.onSuccess {
                _uiState.value = _uiState.value.copy(loading = false)
                onSaved()
            }.onFailure {
                _uiState.value = _uiState.value.copy(loading = false, error = it.message ?: "儲存失敗")
            }
        }
    }

    private fun updateMeterRows(meters: List<Meter>) {
        _uiState.value = _uiState.value.copy(
            rows = meters.map { meter ->
                MeterReadings(meter = meter, readings = readingsByMeterId[meter.id].orEmpty())
            }
        )
        refreshAllowedState()
    }

    private fun refreshAllowedState() {
        val state = _uiState.value
        val date = runCatching { LocalDate.parse(state.recordedDate, formatter) }.getOrNull()
        _uiState.value = state.copy(
            allowedByMeterId = state.rows.associate { row ->
                row.meter.id to (date == null || canRecordReadingOn(row.readings, date))
            }
        )
    }

    class Factory(
        private val uid: String,
        private val buildingId: String,
        private val meterType: MeterType
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return BuildingReadingViewModel(uid, buildingId, meterType) as T
        }
    }
}

data class BuildingReadingUiState(
    val loading: Boolean = false,
    val recordedDate: String = "",
    val note: String = "",
    val rows: List<MeterReadings> = emptyList(),
    val valuesByMeterId: Map<String, String> = emptyMap(),
    val allowedByMeterId: Map<String, Boolean> = emptyMap(),
    val error: String? = null
)
