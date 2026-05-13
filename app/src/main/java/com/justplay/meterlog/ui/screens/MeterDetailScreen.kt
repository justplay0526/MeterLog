package com.justplay.meterlog.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.justplay.meterlog.data.ReadingWithUsage
import com.justplay.meterlog.ui.components.MeterIcon
import com.justplay.meterlog.ui.components.TrendChart
import com.justplay.meterlog.ui.format.formatDate
import com.justplay.meterlog.ui.format.formatNumber
import com.justplay.meterlog.ui.format.formatUiDate
import com.justplay.meterlog.ui.viewmodel.MeterDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeterDetailScreen(
    uid: String,
    buildingId: String,
    meterId: String,
    onBack: () -> Unit,
    onEditMeter: () -> Unit
) {
    val viewModel: MeterDetailViewModel = viewModel(
        key = "meter-detail-$buildingId-$meterId",
        factory = MeterDetailViewModel.Factory(uid, buildingId, meterId)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(state.meter?.name ?: "表具詳情") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onEditMeter) {
                        Icon(Icons.Rounded.Edit, contentDescription = "編輯表具")
                    }
                }
            )
        }
    ) { padding ->
        if (state.loading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                state.meter?.let { meter ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    MeterIcon(meter.type)
                                    Text(meter.name, style = MaterialTheme.typography.titleLarge)
                                }
                                AssistChip(onClick = {}, label = { Text("${meter.floorNumber}F") })
                            }
                            Text(meter.type.label)
                            if (meter.note.isNotBlank()) {
                                Text(meter.note, style = MaterialTheme.typography.bodyMedium)
                            }
                            Button(onClick = onEditMeter, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Rounded.Edit, contentDescription = null)
                                Spacer(Modifier.padding(horizontal = 4.dp))
                                Text("編輯表具")
                            }
                        }
                    }
                }
            }
            item {
                ReadingScheduleCard(
                    latestDate = state.latestReadingDate?.formatUiDate(),
                    nextAllowedDate = state.nextAllowedReadingDate?.formatUiDate()
                )
            }
            item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(16.dp)) {
                        Text("每期用量", style = MaterialTheme.typography.titleMedium)
                        TrendChart(readings = state.readings)
                    }
                }
            }
            if (state.readings.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("還沒有讀數", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("請從整棟抄表頁新增本次讀數。")
                    }
                }
            } else {
                items(state.readings, key = { it.reading.id }) { reading ->
                    ReadingRow(reading)
                }
            }
        }
    }
}

@Composable
private fun ReadingScheduleCard(latestDate: String?, nextAllowedDate: String?) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (latestDate == null || nextAllowedDate == null) {
                Text("尚無抄表紀錄", style = MaterialTheme.typography.titleMedium)
                Text("第一筆讀數可在整棟抄表頁直接新增。")
            } else {
                Text("最新抄表：$latestDate", style = MaterialTheme.typography.titleMedium)
                Text("下次可抄表：$nextAllowedDate")
            }
        }
    }
}

@Composable
private fun ReadingRow(readingWithUsage: ReadingWithUsage) {
    val reading = readingWithUsage.reading
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(reading.value.formatNumber(), style = MaterialTheme.typography.titleLarge)
                Text(reading.recordedAt.formatDate(), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            when {
                readingWithUsage.isAnomaly -> Text(
                    "異常：小於前次讀數",
                    color = MaterialTheme.colorScheme.error
                )
                readingWithUsage.usageFromPrevious != null -> Text(
                    "本期用量 ${readingWithUsage.usageFromPrevious.formatNumber()}",
                    color = MaterialTheme.colorScheme.primary
                )
                else -> Text("第一筆讀數")
            }
            if (reading.note.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(reading.note, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
