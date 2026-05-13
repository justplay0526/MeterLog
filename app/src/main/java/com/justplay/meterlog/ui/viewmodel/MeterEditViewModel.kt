package com.justplay.meterlog.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.justplay.meterlog.data.Meter
import com.justplay.meterlog.data.MeterRepository
import com.justplay.meterlog.data.MeterType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MeterEditViewModel(
    private val uid: String,
    private val buildingId: String,
    private val meterId: String,
    private val repository: MeterRepository = MeterRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(MeterEditUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeBuildingMeter(uid, buildingId, meterId).collect { meter ->
                if (meter != null) {
                    _uiState.value = MeterEditUiState(
                        name = meter.name,
                        type = meter.type,
                        note = meter.note,
                        meter = meter
                    )
                }
            }
        }
    }

    fun updateName(value: String) {
        _uiState.value = _uiState.value.copy(name = value)
    }

    fun updateType(value: MeterType) {
        _uiState.value = _uiState.value.copy(type = value)
    }

    fun updateNote(value: String) {
        _uiState.value = _uiState.value.copy(note = value)
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        val meter = state.meter
        if (state.name.isBlank()) {
            _uiState.value = state.copy(error = "表具名稱不可空白")
            return
        }
        if (meter == null) {
            _uiState.value = state.copy(error = "找不到表具")
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(loading = true, error = null)
            runCatching {
                repository.updateBuildingMeter(
                    uid = uid,
                    buildingId = buildingId,
                    meter = meter.copy(name = state.name, type = state.type, note = state.note)
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(loading = false)
                onSaved()
            }.onFailure {
                _uiState.value = _uiState.value.copy(loading = false, error = it.message ?: "儲存失敗")
            }
        }
    }

    class Factory(
        private val uid: String,
        private val buildingId: String,
        private val meterId: String
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return MeterEditViewModel(uid, buildingId, meterId) as T
        }
    }
}

data class MeterEditUiState(
    val loading: Boolean = false,
    val name: String = "",
    val type: MeterType = MeterType.WATER,
    val note: String = "",
    val meter: Meter? = null,
    val error: String? = null
)
