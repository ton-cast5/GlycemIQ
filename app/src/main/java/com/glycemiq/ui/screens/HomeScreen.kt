package com.glycemiq.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glycemiq.R
import com.glycemiq.ui.components.EmptyStateMessage
import com.glycemiq.ui.components.GlucoseRecordItem
import com.glycemiq.ui.components.GlycemCard
import com.glycemiq.ui.components.SectionTitle
import com.glycemiq.ui.viewmodel.HomeViewModel
import com.glycemiq.util.DateTimeUtils

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_glycemiq),
                contentDescription = "Logo GlycemIQ",
                modifier = Modifier.size(56.dp)
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = "GlycemIQ",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Monitoreo inteligente de glucosa",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        state.recommendation?.let { recommendation ->
            GlycemCard {
                SectionTitle("Recomendación")
                Text(
                    text = recommendation.message,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (recommendation.medications.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    recommendation.medications.forEach { med ->
                        Text(
                            text = "• $med",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        SectionTitle("Registros recientes", compact = true)

        if (state.recentRecords.isEmpty()) {
            EmptyStateMessage("Aún no hay registros. Agrega tu primer nivel de glucosa.")
        } else {
            state.recentRecords.forEach { record ->
                GlucoseRecordItem(record = record)
            }
        }

        if (state.medications.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            SectionTitle("Medicamentos activos")
            state.medications.forEach { med ->
                GlycemCard(modifier = Modifier.padding(bottom = 12.dp)) {
                    Text(
                        text = med.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Dosis: ${med.dose} — ${DateTimeUtils.formatHourMinute(med.scheduledHour, med.scheduledMinute)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
