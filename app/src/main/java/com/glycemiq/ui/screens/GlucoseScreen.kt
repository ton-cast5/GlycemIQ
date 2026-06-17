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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glycemiq.domain.model.GlucoseContext
import com.glycemiq.ui.components.GlucoseLevelBadge
import com.glycemiq.ui.components.GlucoseRecordItem
import com.glycemiq.ui.components.GlycemCard
import com.glycemiq.ui.components.GlycemNumericField
import com.glycemiq.ui.components.GlycemPrimaryButton
import com.glycemiq.ui.components.HorizontalChipSelector
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
            kotlinx.coroutines.delay(2500)
            viewModel.clearMessages()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        SectionTitle("Registrar glucosa", compact = true)

        formState.errorMessage?.let {
            MessageBanner(message = it, isError = true)
            Spacer(modifier = Modifier.height(8.dp))
        }
        formState.successMessage?.let {
            MessageBanner(message = it, isError = false)
            Spacer(modifier = Modifier.height(8.dp))
        }

        GlycemCard(contentPadding = 12.dp) {
            GlycemNumericField(
                value = formState.value,
                onValueChange = viewModel::updateValue,
                label = "Glucosa (mg/dL)"
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Contexto",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            val contexts = GlucoseContext.entries
            HorizontalChipSelector(
                options = contexts.map { it.label },
                selectedIndex = contexts.indexOf(formState.context),
                onSelected = { index -> viewModel.updateContext(contexts[index]) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DateTimeUtils.formatDateTime(formState.timestamp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                formState.previewLevel?.let { level ->
                    val value = formState.value.toIntOrNull() ?: 0
                    GlucoseLevelBadge(level = level, value = value, compact = true)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            GlycemPrimaryButton(
                text = if (formState.isSaving) "Guardando..." else "Guardar",
                onClick = viewModel::saveRecord,
                enabled = !formState.isSaving,
                compact = true
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        SectionTitle("Historial", compact = true)

        if (listState.records.isEmpty()) {
            Text(
                text = "Sin registros aún.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            listState.records.forEach { record ->
                GlucoseRecordItem(
                    record = record,
                    onDelete = { viewModel.deleteRecord(record.id) }
                )
            }
        }
    }
}
