package com.glycemiq.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glycemiq.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    hour: Int,
    minute: Int,
    onTimeSelected: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Hora programada",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = DateTimeUtils.formatHourMinute(hour, minute),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = "Seleccionar hora",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showDialog) {
        val state = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Seleccionar hora") },
            text = {
                TimePicker(
                    state = state,
                    layoutType = TimePickerLayoutType.Vertical,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onTimeSelected(state.hour, state.minute)
                    showDialog = false
                }) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
