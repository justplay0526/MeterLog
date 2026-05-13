package com.justplay.meterlog.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.justplay.meterlog.data.Meter
import com.justplay.meterlog.data.MeterRepository
import com.justplay.meterlog.data.ReadingWithUsage
import com.justplay.meterlog.data.latestReadingDate
import com.justplay.meterlog.data.nextAllowedReadingDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class MeterDetailViewModel(
    uid: String,
    buildingId: String,
    meterId: String,
    repository: MeterRepository = MeterRepository()
) : ViewModel() {
    val uiState: StateFlow<MeterDetailUiState> = combine(
        repository.observeBuildingMeter(uid, buildingId, meterId),
        repository.observeBuildingReadings(uid, buildingId, meterId)
    ) { meter, readings ->
        val ascending = readings.sortedBy { it.recordedAt?.seconds ?: 0L }
        val usageById = ascending.mapIndexed { index, reading ->
            val previous = ascending.getOrNull(index - 1)
            val usage = previous?.let { reading.value - it.value }
            reading.id to ReadingWithUsage(
                reading = reading,
                usageFromPrevious = usage,
                isAnomaly = usage != null && usage < 0
            )
        }.toMap()
        MeterDetailUiState(
            loading = false,
            meter = meter,
            readings = readings.mapNotNull { usageById[it.id] },
            latestReadingDate = latestReadingDate(readings),
            nextAllowedReadingDate = nextAllowedReadingDate(readings)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MeterDetailUiState(loading = true)
    )

    class Factory(
        private val uid: String,
        private val buildingId: String,
        private val meterId: String
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return MeterDetailViewModel(uid, buildingId, meterId) as T
        }
    }
}

data class MeterDetailUiState(
    val loading: Boolean = false,
    val meter: Meter? = null,
    val readings: List<ReadingWithUsage> = emptyList(),
    val latestReadingDate: LocalDate? = null,
    val nextAllowedReadingDate: LocalDate? = null
)
