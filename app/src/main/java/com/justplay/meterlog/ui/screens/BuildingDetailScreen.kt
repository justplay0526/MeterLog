package com.justplay.meterlog.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.justplay.meterlog.data.Meter
import com.justplay.meterlog.data.MeterType
import com.justplay.meterlog.ui.components.MeterIcon
import com.justplay.meterlog.ui.viewmodel.BuildingDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingDetailScreen(
    uid: String,
    buildingId: String,
    onBack: () -> Unit,
    onOpenMeter: (String) -> Unit,
    onBatchReading: (MeterType) -> Unit
) {
    val viewModel: BuildingDetailViewModel = viewModel(
        key = "building-detail-$buildingId",
        factory = BuildingDetailViewModel.Factory(uid, buildingId)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(state.building?.name ?: "建物詳情") },
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
                ReadingActionsCard(onBatchReading = onBatchReading)
            }
            state.metersByFloor.forEach { (floor, meters) ->
                item(key = "floor-$floor") {
                    Text("${floor}F", style = MaterialTheme.typography.titleLarge)
                }
                items(meters, key = { it.id }) { meter ->
                    MeterRow(meter = meter, onClick = { onOpenMeter(meter.id) })
                }
            }
        }
    }
}

@Composable
private fun ReadingActionsCard(onBatchReading: (MeterType) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("新增本次抄表", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onBatchReading(MeterType.WATER) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("抄水表")
                }
                Button(
                    onClick = { onBatchReading(MeterType.ELECTRIC) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("抄電表")
                }
            }
        }
    }
}

@Composable
private fun MeterRow(meter: Meter, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        ListItem(
            headlineContent = { Text(meter.name) },
            supportingContent = { Text(meter.type.label) },
            leadingContent = { MeterIcon(meter.type) },
            trailingContent = { Icon(Icons.Rounded.ChevronRight, contentDescription = "查看詳情") },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}
