package com.glycemiq.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glycemiq.domain.model.MedicationInterval
import com.glycemiq.ui.components.GlycemCard
import com.glycemiq.ui.components.GlycemOutlinedButton
import com.glycemiq.ui.components.GlycemPrimaryButton
import com.glycemiq.ui.components.GlycemTextField
import com.glycemiq.ui.components.HorizontalChipSelector
import com.glycemiq.ui.components.MessageBanner
import com.glycemiq.ui.components.SectionTitle
import com.glycemiq.ui.components.TimePickerField
import com.glycemiq.ui.viewmodel.MedicationViewModel
import com.glycemiq.util.DateTimeUtils

@Composable
fun MedicationsScreen(
    viewModel: MedicationViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsState()
    val listState by viewModel.listState.collectAsState()
    val intervals = MedicationInterval.entries

    LaunchedEffect(formState.successMessage) {
        if (formState.successMessage != null) {
            kotlinx.coroutines.delay(2500)
            viewModel.clearMessages()
            viewModel.resetForm()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        SectionTitle(
            if (formState.editingId != null) "Editar medicamento" else "Nuevo medicamento",
            compact = true
        )

        formState.errorMessage?.let {
            MessageBanner(message = it, isError = true)
            Spacer(modifier = Modifier.height(8.dp))
        }
        formState.successMessage?.let {
            MessageBanner(message = it, isError = false)
            Spacer(modifier = Modifier.height(8.dp))
        }

        GlycemCard(contentPadding = 12.dp) {
            GlycemTextField(
                value = formState.name,
                onValueChange = viewModel::updateName,
                label = "Nombre"
            )
            Spacer(modifier = Modifier.height(8.dp))
            GlycemTextField(
                value = formState.dose,
                onValueChange = viewModel::updateDose,
                label = "Dosis"
            )
            Spacer(modifier = Modifier.height(10.dp))

            TimePickerField(
                hour = formState.hour,
                minute = formState.minute,
                onTimeSelected = viewModel::updateTime
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Repetir notificación",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalChipSelector(
                options = intervals.map { it.label },
                selectedIndex = intervals.indexOf(formState.interval),
                onSelected = { index -> viewModel.updateInterval(intervals[index]) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = formState.recommendForHighGlucose,
                    onCheckedChange = viewModel::updateRecommendForHigh
                )
                Text(
                    text = "Recomendar si tengo glucosa alta",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            GlycemPrimaryButton(
                text = if (formState.isSaving) "Guardando..." else "Guardar",
                onClick = viewModel::saveMedication,
                enabled = !formState.isSaving,
                compact = true
            )

            if (formState.editingId != null) {
                Spacer(modifier = Modifier.height(6.dp))
                GlycemOutlinedButton(text = "Cancelar", onClick = viewModel::resetForm)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        SectionTitle("Mis medicamentos", compact = true)

        if (listState.medications.isEmpty()) {
            Text(
                text = "No hay medicamentos registrados.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            listState.medications.forEach { medication ->
                GlycemCard(
                    modifier = Modifier.padding(bottom = 8.dp),
                    contentPadding = 12.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = medication.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Dosis: ${medication.dose}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${DateTimeUtils.formatHourMinute(medication.scheduledHour, medication.scheduledMinute)} · Cada ${medication.intervalHours}h",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (medication.recommendForHighGlucose) {
                                Text(
                                    text = "✓ Recomendado para glucosa alta",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Switch(
                                checked = medication.isActive,
                                onCheckedChange = { viewModel.toggleActive(medication) }
                            )
                            Row {
                                IconButton(onClick = { viewModel.startEditing(medication) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                                }
                                IconButton(onClick = { viewModel.deleteMedication(medication.id) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Eliminar",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
