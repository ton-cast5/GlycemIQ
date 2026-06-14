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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.glycemiq.domain.model.GlucoseContext
import com.glycemiq.ui.components.ChipSelector
import com.glycemiq.ui.components.EmptyStateMessage
import com.glycemiq.ui.components.GlucoseLevelBadge
import com.glycemiq.ui.components.GlycemCard
import com.glycemiq.ui.components.GlycemPrimaryButton
import com.glycemiq.ui.components.GlycemTextField
import com.glycemiq.ui.components.MessageBanner
import com.glycemiq.ui.components.SectionTitle
import com.glycemiq.ui.viewmodel.GlucoseViewModel
import com.glycemiq.util.DateTimeUtils

@Composable
fun GlucoseScreen(
    viewModel: GlucoseViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsState()
    val listState by viewModel.listState.collectAsState()

    LaunchedEffect(formState.successMessage) {
        if (formState.successMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        SectionTitle("Registrar glucosa")

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
                value = formState.value,
                onValueChange = viewModel::updateValue,
                label = "Nivel de glucosa (mg/dL)"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Contexto",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            val contexts = GlucoseContext.entries
            ChipSelector(
                options = contexts.map { it.label },
                selectedIndex = contexts.indexOf(formState.context),
                onSelected = { index -> viewModel.updateContext(contexts[index]) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Fecha y hora: ${DateTimeUtils.formatDateTime(formState.timestamp)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            formState.previewLevel?.let { level ->
                Spacer(modifier = Modifier.height(12.dp))
                val value = formState.value.toIntOrNull() ?: 0
                GlucoseLevelBadge(level = level, value = value)
            }

            Spacer(modifier = Modifier.height(20.dp))

            GlycemPrimaryButton(
                text = if (formState.isSaving) "Guardando..." else "Guardar registro",
                onClick = viewModel::saveRecord,
                enabled = !formState.isSaving
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        SectionTitle("Historial")

        if (listState.records.isEmpty()) {
            EmptyStateMessage("No hay registros en el historial.")
        } else {
            listState.records.forEach { record ->
                GlycemCard(modifier = Modifier.padding(bottom = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = DateTimeUtils.formatDateTime(record.timestamp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = record.context.label,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            GlucoseLevelBadge(level = record.level, value = record.value)
                        }
                        IconButton(onClick = { viewModel.deleteRecord(record.id) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
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
