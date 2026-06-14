package com.glycemiq.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.glycemiq.ui.components.GlycemCard
import com.glycemiq.ui.components.GlycemPrimaryButton
import com.glycemiq.ui.components.MessageBanner
import com.glycemiq.ui.components.SectionTitle
import com.glycemiq.ui.viewmodel.ReportViewModel

@Composable
fun ReportScreen(
    viewModel: ReportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.successMessage, state.errorMessage) {
        if (state.successMessage != null || state.errorMessage != null) {
            kotlinx.coroutines.delay(4000)
            viewModel.clearMessages()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        SectionTitle("Reporte clínico PDF")

        state.errorMessage?.let {
            MessageBanner(message = it, isError = true)
            Spacer(modifier = Modifier.height(12.dp))
        }
        state.successMessage?.let {
            MessageBanner(message = it, isError = false)
            Spacer(modifier = Modifier.height(12.dp))
        }

        GlycemCard {
            Text(
                text = "GlycemIQ",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Reporte de monitoreo glucémico del paciente, generado automáticamente " +
                    "con base en los registros almacenados en la aplicación.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Registros disponibles: ${state.recordCount}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "El documento incluirá: Fecha, Hora, Nivel de glucosa y Contexto " +
                    "(ayunas / antes / después de comer).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            GlycemPrimaryButton(
                text = if (state.isGenerating) "Generando..." else "Generar y compartir PDF",
                onClick = {
                    viewModel.generateReport { file ->
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(
                            Intent.createChooser(shareIntent, "Compartir reporte PDF")
                        )
                    }
                },
                enabled = !state.isGenerating && state.recordCount > 0
            )
        }
    }
}
