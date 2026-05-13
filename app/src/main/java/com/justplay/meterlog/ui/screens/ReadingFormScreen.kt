package com.justplay.meterlog.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.justplay.meterlog.ui.format.formatUiDate
import com.justplay.meterlog.ui.viewmodel.ReadingFormViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingFormScreen(
    uid: String,
    meterId: String,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val viewModel: ReadingFormViewModel = viewModel(
        key = "reading-form-$meterId",
        factory = ReadingFormViewModel.Factory(uid, meterId)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("新增讀數") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ReadingIntervalInfo(
                latestDate = state.latestReadingDate?.formatUiDate(),
                nextAllowedDate = state.nextAllowedReadingDate?.formatUiDate()
            )
            OutlinedTextField(
                value = state.value,
                onValueChange = viewModel::updateValue,
                label = { Text("目前讀數") },
                placeholder = { Text("例如 0145") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            OutlinedTextField(
                value = state.recordedDate,
                onValueChange = viewModel::updateRecordedDate,
                label = { Text("記錄日期") },
                placeholder = { Text("YYYY-MM-DD") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::updateNote,
                label = { Text("備註") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
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
                Text(if (state.loading) "儲存中" else "儲存讀數")
            }
        }
    }
}

@Composable
private fun ReadingIntervalInfo(latestDate: String?, nextAllowedDate: String?) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            if (latestDate == null || nextAllowedDate == null) {
                Text("第一筆讀數可直接新增")
                Spacer(Modifier.height(6.dp))
                Text("之後每次抄表需距離上次至少 6 週。")
            } else {
                Text("最新抄表：$latestDate")
                Spacer(Modifier.height(6.dp))
                Text("下次可抄表：$nextAllowedDate")
            }
        }
    }
}
