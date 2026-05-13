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

class MeterFormViewModel(
    private val uid: String,
    private val meterId: String?,
    private val repository: MeterRepository = MeterRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(MeterFormUiState())
    val uiState = _uiState.asStateFlow()

    init {
        if (!meterId.isNullOrBlank()) {
            viewModelScope.launch {
                repository.observeMeter(uid, meterId).collect { meter ->
                    if (meter != null) {
                        _uiState.value = MeterFormUiState(
                            name = meter.name,
                            type = meter.type,
                            locationLabel = meter.locationLabel,
                            note = meter.note,
                            original = meter
                        )
                    }
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

    fun updateLocation(value: String) {
        _uiState.value = _uiState.value.copy(locationLabel = value)
    }

    fun updateNote(value: String) {
        _uiState.value = _uiState.value.copy(note = value)
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.value = state.copy(error = "表具名稱不可空白")
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(loading = true, error = null)
            runCatching {
                repository.upsertMeter(
                    uid = uid,
                    meter = Meter(
                        id = state.original?.id ?: meterId.orEmpty(),
                        name = state.name,
                        type = state.type,
                        locationLabel = state.locationLabel,
                        note = state.note,
                        createdAt = state.original?.createdAt
                    )
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(loading = false)
                onSaved()
            }.onFailure {
                _uiState.value = _uiState.value.copy(loading = false, error = it.message ?: "儲存失敗")
            }
        }
    }

    class Factory(private val uid: String, private val meterId: String?) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return MeterFormViewModel(uid, meterId) as T
        }
    }
}

data class MeterFormUiState(
    val loading: Boolean = false,
    val name: String = "",
    val type: MeterType = MeterType.WATER,
    val locationLabel: String = "",
    val note: String = "",
    val original: Meter? = null,
    val error: String? = null
)
