package com.glycemiq.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.glycemiq.ui.components.EmptyStateMessage
import com.glycemiq.ui.components.GlycemCard
import com.glycemiq.ui.components.GlycemOutlinedButton
import com.glycemiq.ui.components.GlycemPrimaryButton
import com.glycemiq.ui.components.GlycemTextField
import com.glycemiq.ui.components.MessageBanner
import com.glycemiq.ui.components.SectionTitle
import com.glycemiq.ui.viewmodel.MedicationViewModel
import com.glycemiq.util.DateTimeUtils

@Composable
fun MedicationsScreen(
    viewModel: MedicationViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsState()
    val listState by viewModel.listState.collectAsState()

    LaunchedEffect(formState.successMessage) {
        if (formState.successMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
            viewModel.resetForm()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        SectionTitle(
            if (formState.editingId != null) "Editar medicamento" else "Nuevo medicamento"
        )

        formState.errorMessage?.let {
            MessageBanner(message = it, isError = true)
            Spacer(modifier = Modifier.height(12.dp))
        }
        formState.successMessage?.let {
            MessageBanner(message = it, isError = false)
            Spacer(modifier = Modifier.height(12.dp))
        }

        GlycemCard {
            GlycemTextField(
                value = formState.name,
                onValueChange = viewModel::updateName,
                label = "Nombre del medicamento"
            )
            Spacer(modifier = Modifier.height(12.dp))
            GlycemTextField(
                value = formState.dose,
                onValueChange = viewModel::updateDose,
                label = "Dosis"
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Hora programada",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlycemTextField(
                    value = formState.hour,
                    onValueChange = viewModel::updateHour,
                    label = "Hora",
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = ":",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                GlycemTextField(
                    value = formState.minute,
                    onValueChange = viewModel::updateMinute,
                    label = "Min",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Recibirás una notificación: \"Es momento de tomar tu medicamento: [nombre]\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            GlycemPrimaryButton(
                text = if (formState.isSaving) "Guardando..." else "Guardar medicamento",
                onClick = viewModel::saveMedication,
                enabled = !formState.isSaving
            )

            if (formState.editingId != null) {
                Spacer(modifier = Modifier.height(8.dp))
                GlycemOutlinedButton(
                    text = "Cancelar edición",
                    onClick = viewModel::resetForm
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        SectionTitle("Mis medicamentos")

        if (listState.medications.isEmpty()) {
            EmptyStateMessage("No hay medicamentos registrados.")
        } else {
            listState.medications.forEach { medication ->
                GlycemCard(modifier = Modifier.padding(bottom = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
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
                                text = "Hora: ${DateTimeUtils.formatHourMinute(medication.scheduledHour, medication.scheduledMinute)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = medication.isActive,
                                onCheckedChange = { viewModel.toggleActive(medication) }
                            )
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
