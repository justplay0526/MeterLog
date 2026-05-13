package com.justplay.meterlog.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.justplay.meterlog.data.MeterReadings
import com.justplay.meterlog.data.MeterType
import com.justplay.meterlog.ui.components.MeterIcon
import com.justplay.meterlog.ui.format.formatDate
import com.justplay.meterlog.ui.format.formatNumber
import com.justplay.meterlog.ui.format.formatUiDate
import com.justplay.meterlog.ui.viewmodel.BuildingReadingViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingReadingScreen(
    uid: String,
    buildingId: String,
    meterType: MeterType,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val viewModel: BuildingReadingViewModel = viewModel(
        key = "building-reading-$buildingId-${meterType.name}",
        factory = BuildingReadingViewModel.Factory(uid, buildingId, meterType)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val nextAllowedDate = state.rows
        .filter { state.allowedByMeterId[it.meter.id] == false }
        .mapNotNull { it.nextAllowedReadingDate }
        .maxOrNull()
    val hasPreviousReading = state.rows.any { it.latestReading != null }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("新增本次${meterType.label}抄表") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ReadingDateCard(
                    recordedDate = state.recordedDate,
                    nextAllowedDate = nextAllowedDate,
                    hasPreviousReading = hasPreviousReading,
                    onRecordedDateChange = viewModel::updateRecordedDate
                )
            }
            if (state.rows.isEmpty()) {
                item {
                    Text("此建物尚未建立${meterType.label}", style = MaterialTheme.typography.bodyLarge)
                }
            }
            state.rows.groupBy { it.meter.floorNumber }.toSortedMap().forEach { (floor, rows) ->
                item(key = "floor-$floor") {
                    Text("${floor}F", style = MaterialTheme.typography.titleLarge)
                }
                items(rows, key = { it.meter.id }) { row ->
                    ReadingInputRow(
                        row = row,
                        value = state.valuesByMeterId[row.meter.id].orEmpty(),
                        enabled = state.allowedByMeterId[row.meter.id] ?: true,
                        onValueChange = { viewModel.updateValue(row.meter.id, it) }
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = state.note,
                    onValueChange = viewModel::updateNote,
                    label = { Text("本次備註") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
            state.error?.let {
                item {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
            item {
                Button(
                    onClick = { viewModel.save(onSaved) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.loading
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.loading) "儲存中" else "儲存本次${meterType.label}抄表")
                }
            }
        }
    }
}

@Composable
private fun ReadingDateCard(
    recordedDate: String,
    nextAllowedDate: LocalDate?,
    hasPreviousReading: Boolean,
    onRecordedDateChange: (String) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = recordedDate,
                onValueChange = onRecordedDateChange,
                label = { Text("抄表日期") },
                placeholder = { Text("YYYY-MM-DD") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (nextAllowedDate == null && !hasPreviousReading) {
                Text("第一次抄表可直接新增")
            } else if (nextAllowedDate != null) {
                Text(
                    "下次可抄表：${nextAllowedDate.formatUiDate()}",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ReadingInputRow(
    row: MeterReadings,
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    MeterIcon(row.meter.type)
                    Text(row.meter.name, style = MaterialTheme.typography.titleMedium)
                }
                Text(row.meter.type.label)
            }
            row.latestReading?.let { latestReading ->
                Text(
                    text = "上期讀數：${latestReading.value.formatNumber()}（${latestReading.recordedAt.formatDate()}）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                label = { Text("讀數") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        }
    }
}
