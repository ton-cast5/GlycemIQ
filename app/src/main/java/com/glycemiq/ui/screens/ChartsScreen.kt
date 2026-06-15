package com.glycemiq.ui.screens

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glycemiq.ui.components.EmptyStateMessage
import com.glycemiq.ui.components.GlucoseChart
import com.glycemiq.ui.components.GlycemCard
import com.glycemiq.ui.components.HorizontalChipSelector
import com.glycemiq.ui.components.SectionTitle
import com.glycemiq.ui.viewmodel.ChartType
import com.glycemiq.ui.viewmodel.ChartsViewModel

@Composable
fun ChartsScreen(
    viewModel: ChartsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val chartTypes = ChartType.entries

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        SectionTitle("Visualización de glucosa", compact = true)

        Text(
            text = "Verde: normal · Naranja: moderado · Rojo: crítico",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalChipSelector(
            options = chartTypes.map { it.label },
            selectedIndex = chartTypes.indexOf(state.chartType),
            onSelected = { index -> viewModel.setChartType(chartTypes[index]) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (state.isEmpty) {
            EmptyStateMessage("No hay datos suficientes para mostrar gráficas.")
        } else {
            GlycemCard(contentPadding = 12.dp) {
                Text(
                    text = state.chartType.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                GlucoseChart(dataPoints = state.dataPoints)
            }
        }
    }
}
