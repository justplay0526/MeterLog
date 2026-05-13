package com.justplay.meterlog.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.justplay.meterlog.data.MeterRepository
import com.justplay.meterlog.data.Reading
import com.justplay.meterlog.data.canRecordReadingOn
import com.justplay.meterlog.data.latestReadingDate
import com.justplay.meterlog.data.nextAllowedReadingDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ReadingFormViewModel(
    private val uid: String,
    private val meterId: String,
    private val repository: MeterRepository = MeterRepository()
) : ViewModel() {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val _uiState = MutableStateFlow(
        ReadingFormUiState(recordedDate = LocalDate.now().format(formatter))
    )
    val uiState = _uiState.asStateFlow()

    private var existingReadings = emptyList<Reading>()

    init {
        viewModelScope.launch {
            repository.observeReadings(uid, meterId).collect { readings ->
                existingReadings = readings
                _uiState.value = _uiState.value.copy(
                    latestReadingDate = latestReadingDate(readings),
                    nextAllowedReadingDate = nextAllowedReadingDate(readings)
                )
            }
        }
    }

    fun updateValue(value: String) {
        _uiState.value = _uiState.value.copy(value = value)
    }

    fun updateRecordedDate(value: String) {
        _uiState.value = _uiState.value.copy(recordedDate = value)
    }

    fun updateNote(value: String) {
        _uiState.value = _uiState.value.copy(note = value)
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        val readingValue = state.value.toDoubleOrNull()
        if (readingValue == null) {
            _uiState.value = state.copy(error = "讀數必須是有效數字")
            return
        }
        val date = runCatching { LocalDate.parse(state.recordedDate, formatter) }.getOrNull()
        if (date == null) {
            _uiState.value = state.copy(error = "日期格式請使用 YYYY-MM-DD")
            return
        }
        if (!canRecordReadingOn(existingReadings, date)) {
            val nextAllowedDate = nextAllowedReadingDate(existingReadings)?.format(formatter)
            _uiState.value = state.copy(error = "需距離上次抄表至少 2 個月，下次可抄表：$nextAllowedDate")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(loading = true, error = null)
            runCatching {
                val instant = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
                repository.addReading(
                    uid = uid,
                    meterId = meterId,
                    value = readingValue,
                    recordedAt = Timestamp(instant.epochSecond, 0),
                    note = state.note
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(loading = false)
                onSaved()
            }.onFailure {
                _uiState.value = _uiState.value.copy(loading = false, error = it.message ?: "儲存失敗")
            }
        }
    }

    class Factory(private val uid: String, private val meterId: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ReadingFormViewModel(uid, meterId) as T
        }
    }
}

data class ReadingFormUiState(
    val loading: Boolean = false,
    val value: String = "",
    val recordedDate: String = "",
    val note: String = "",
    val latestReadingDate: LocalDate? = null,
    val nextAllowedReadingDate: LocalDate? = null,
    val error: String? = null
)
