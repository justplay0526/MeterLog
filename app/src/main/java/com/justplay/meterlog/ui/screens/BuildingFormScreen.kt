package com.justplay.meterlog.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.OutlinedButton
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
import com.justplay.meterlog.ui.viewmodel.BuildingFormViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingFormScreen(
    uid: String,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val viewModel: BuildingFormViewModel = viewModel(factory = BuildingFormViewModel.Factory(uid))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val floorCount = state.floorCountText.toIntOrNull()?.coerceIn(1, 99) ?: 1

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("新增建物") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::updateName,
                label = { Text("建物名稱") },
                placeholder = { Text("例如 A棟") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = state.floorCountText,
                onValueChange = viewModel::updateFloorCount,
                label = { Text("樓層數") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            DefaultMeterCountCard(
                waterCount = state.defaultWaterCount,
                electricCount = state.defaultElectricCount,
                onWaterCountChange = viewModel::updateDefaultWaterCount,
                onElectricCountChange = viewModel::updateDefaultElectricCount,
                onApply = viewModel::applyDefaultsToAllFloors
            )
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("逐層表具數量", style = MaterialTheme.typography.titleMedium)
                    (1..floorCount).forEach { floor ->
                        FloorMeterCountRow(
                            floor = floor,
                            waterCount = state.waterCountsByFloor[floor].orEmpty(),
                            electricCount = state.electricCountsByFloor[floor].orEmpty(),
                            onWaterCountChange = { viewModel.updateWaterCount(floor, it) },
                            onElectricCountChange = { viewModel.updateElectricCount(floor, it) }
                        )
                    }
                }
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.save(onSaved) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.loading
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (state.loading) "建立中" else "建立建物")
            }
        }
    }
}

@Composable
private fun DefaultMeterCountCard(
    waterCount: String,
    electricCount: String,
    onWaterCountChange: (String) -> Unit,
    onElectricCountChange: (String) -> Unit,
    onApply: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("快捷設定", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CountField(
                    label = "每層水表",
                    value = waterCount,
                    onValueChange = onWaterCountChange,
                    modifier = Modifier.weight(1f)
                )
                CountField(
                    label = "每層電表",
                    value = electricCount,
                    onValueChange = onElectricCountChange,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedButton(onClick = onApply, modifier = Modifier.fillMaxWidth()) {
                Text("套用到全部樓層")
            }
        }
    }
}

@Composable
private fun FloorMeterCountRow(
    floor: Int,
    waterCount: String,
    electricCount: String,
    onWaterCountChange: (String) -> Unit,
    onElectricCountChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("${floor}F", modifier = Modifier.width(44.dp))
        CountField(
            label = "水表",
            value = waterCount,
            onValueChange = onWaterCountChange,
            modifier = Modifier.weight(1f)
        )
        CountField(
            label = "電表",
            value = electricCount,
            onValueChange = onElectricCountChange,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CountField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}
